// SPDX-License-Identifier: GPL-3.0-or-later
package com.xenris.liquidwarsos.cloud;

import java.util.*;
import org.json.*;

/** Immutable collision-derived visibility regions and portals. No AI, RNG,
 * enemy preferences or runtime rerouting. Each region is connected and its
 * portal link geometry is fixed before any player observation exists. */
public final class ModelNavigation {
 public static final int CELL=4;public final int W,H,COLS,ROWS,N;
 private final byte[] walls;
 private final int[] pixelRoom;
 private final int[] x,y,region,clearance;
 private final byte[] edges;
 public final ArrayList<Room> rooms=new ArrayList<>();
 public final ArrayList<Door> doors=new ArrayList<>();
 public final String description;
 public final long buildMs;
 public final boolean connectedRepair;
 public static final class Room {public final int id,x,y;public int area;Room(int id,int x,int y){this.id=id;this.x=x;this.y=y;}}
 public static final class Door {
  public final int id,a,b,x,y,width,ax,ay,bx,by;public final double length;public final int[][] pathA,pathB;
  Door(int id,int a,int b,int x,int y,int width,int ax,int ay,int bx,int by,int[][] pa,int[][] pb){this.id=id;this.a=a;this.b=b;this.x=x;this.y=y;this.width=width;this.ax=ax;this.ay=ay;this.bx=bx;this.by=by;this.pathA=pa;this.pathB=pb;double len=Math.hypot(ax-bx,ay-by);for(int[][] path:new int[][][]{pa,pb})for(int k=1;k<path.length;k++)len+=Math.hypot(path[k][0]-path[k-1][0],path[k][1]-path[k-1][1]);this.length=len;}
  public int other(int room){return room==a?b:room==b?a:-1;}
 }
 public static final class Route {public final int[] doors,rooms;public final double length;Route(int[] d,int[] r,double l){doors=d;rooms=r;length=l;}}
 private static final int[] DX={1,-1,0,0},DY={0,0,1,-1};
 public ModelNavigation(byte[] input){this(input,800,480);}
 public ModelNavigation(byte[] input,int width,int height){W=width;H=height;COLS=(W+CELL-1)/CELL;ROWS=(H+CELL-1)/CELL;N=COLS*ROWS;pixelRoom=new int[W*H];x=new int[N];y=new int[N];region=new int[N];clearance=new int[N];edges=new byte[N];
  long started=System.nanoTime();if(input==null||input.length!=W*H)throw new IllegalArgumentException("Terrain size");walls=input.clone();Arrays.fill(region,-1);Arrays.fill(clearance,100000);
  for(int c=0;c<N;c++){
   int bx=c%COLS*CELL,by=c/COLS*CELL,best=10000;x[c]=y[c]=-1;
   for(int yy=by;yy<by+CELL;yy++)for(int xx=bx;xx<bx+CELL;xx++)if(!wall(xx,yy)){int d=(xx-bx-CELL/2)*(xx-bx-CELL/2)+(yy-by-CELL/2)*(yy-by-CELL/2);if(d<best){best=d;x[c]=xx;y[c]=yy;}}
  }
  for(int c=0;c<N;c++)if(x[c]>=0)for(int k=0;k<4;k++){int q=neighbor(c,k);if(q>=0&&x[q]>=0&&clear(x[c],y[c],x[q],y[q]))edges[c]|=1<<k;}
  // A curved border can leave a 1-2px fringe as a separate coarse-grid node.
  // Do not invent disconnected rooms for those fringes; exact pixel flooding
  // below attaches them to the real region without changing collision data.
  boolean[] visited=new boolean[N];ArrayList<int[]> components=new ArrayList<>();
  for(int c=0;c<N;c++)if(x[c]>=0&&!visited[c]){int[] cells=new int[N];int h=0,n=0;cells[n++]=c;visited[c]=true;while(h<n){int at=cells[h++];for(int k=0;k<4;k++)if((edges[at]&(1<<k))!=0){int q=neighbor(at,k);if(!visited[q]){visited[q]=true;cells[n++]=q;}}}components.add(Arrays.copyOf(cells,n));}
  if(components.size()>1){components.sort((a,b)->Integer.compare(b.length,a.length));for(int i=1;i<components.size();i++)if(components.get(i).length<=4)for(int c:components.get(i)){x[c]=y[c]=-1;edges[c]=0;}}
  int[] queue=new int[N];int head=0,tail=0;
  for(int c=0;c<N;c++)if(x[c]<0||Integer.bitCount(edges[c]&15)<4){clearance[c]=0;queue[tail++]=c;}
  while(head<tail){int c=queue[head++];for(int k=0;k<4;k++){int q=neighbor(c,k);if(q>=0&&clearance[q]>clearance[c]+1){clearance[q]=clearance[c]+1;queue[tail++]=q;}}}
  // Each growth is star-shaped about its seed. Thin/curved leftovers become
  // additional explicit regions, rather than invisible guessed corridors.
  for(;;){int seed=-1;for(int c=0;c<N;c++)if(x[c]>=0&&region[c]<0&&(seed<0||clearance[c]>clearance[seed]))seed=c;if(seed<0)break;
   Room room=new Room(rooms.size(),x[seed],y[seed]);rooms.add(room);head=tail=0;queue[tail++]=seed;region[seed]=room.id;
   while(head<tail){int c=queue[head++];for(int k=0;k<4;k++)if((edges[c]&(1<<k))!=0){int q=neighbor(c,k);if(region[q]<0&&clear(room.x,room.y,x[q],y[q])){region[q]=room.id;queue[tail++]=q;}}}
  }
  Arrays.fill(pixelRoom,-1);int[] pixels=new int[W*H];head=tail=0;
  for(int c=0;c<N;c++)if(region[c]>=0){int at=y[c]*W+x[c];pixelRoom[at]=region[c];pixels[tail++]=at;}
  while(head<tail){int at=pixels[head++],xx=at%W,yy=at/W;for(int k=0;k<4;k++){int nx=xx+DX[k],ny=yy+DY[k];if(wall(nx,ny))continue;int q=ny*W+nx;if(pixelRoom[q]<0){pixelRoom[q]=pixelRoom[at];pixels[tail++]=q;}}}
  // Multiple coarse representatives can have their thin connecting pixels
  // claimed by a competing region's wavefront. Detect this before constructing
  // portals. Repair only the navigation labels, never the collision map.
  boolean[] connected=new boolean[W*H];head=tail=0;
  for(Room r:rooms){int at=r.y*W+r.x;connected[at]=true;pixels[tail++]=at;}
  while(head<tail){int at=pixels[head++],px=at%W,py=at/W;for(int k=0;k<4;k++){int nx=px+DX[k],ny=py+DY[k];if(wall(nx,ny))continue;int q=ny*W+nx;if(!connected[q]&&pixelRoom[q]==pixelRoom[at]){connected[q]=true;pixels[tail++]=q;}}}
  boolean fragmented=false;for(int at=0;at<W*H;at++)if(walls[at]==0&&!connected[at]){fragmented=true;break;}
  connectedRepair=fragmented;
  if(fragmented){
   // Keep every already connected region intact. Split only disconnected
   // fragments, rather than regrowing all labels and multiplying tiny rooms.
   // Fixed intra-region BFS below handles any non-visible boundary fringe.
   for(int seed=0;seed<W*H;seed++)if(walls[seed]==0&&!connected[seed]){
    int old=pixelRoom[seed],best=seed;head=0;tail=1;pixels[0]=seed;connected[seed]=true;
    while(head<tail){int at=pixels[head++],px=at%W,py=at/W;int c=py/CELL*COLS+px/CELL,b=(best/W)/CELL*COLS+(best%W)/CELL;if(clearance[c]>clearance[b])best=at;
     for(int k=0;k<4;k++){int nx=px+DX[k],ny=py+DY[k];if(wall(nx,ny))continue;int q=ny*W+nx;if(!connected[q]&&pixelRoom[q]==old){connected[q]=true;pixels[tail++]=q;}}
    }
    Room r=new Room(rooms.size(),best%W,best/W);rooms.add(r);for(int i=0;i<tail;i++)pixelRoom[pixels[i]]=r.id;
   }
  }
  for(int at=0;at<W*H;at++)if(walls[at]==0){if(pixelRoom[at]<0)throw new IllegalArgumentException("Unrepresented floor component");rooms.get(pixelRoom[at]).area++;}
  // Boundary components for each region pair become independently selectable
  // doors; prefer their widest actual crossing as a reproducible center.
  Map<Long,ArrayList<int[]>> boundaries=new TreeMap<>();
  for(int py=0;py<H;py++)for(int px=0;px<W;px++){int c=py*W+px;if(pixelRoom[c]<0)continue;for(int k:new int[]{0,2}){int nx=px+DX[k],ny=py+DY[k];if(wall(nx,ny))continue;int q=ny*W+nx;if(pixelRoom[c]==pixelRoom[q])continue;int a=Math.min(pixelRoom[c],pixelRoom[q]),b=Math.max(pixelRoom[c],pixelRoom[q]);long key=((long)a<<32)|b;boundaries.computeIfAbsent(key,v->new ArrayList<>()).add(pixelRoom[c]==a?new int[]{c,q}:new int[]{q,c});}}
  int[] parents=new int[W*H];
  for(Map.Entry<Long,ArrayList<int[]>> entry:boundaries.entrySet()){
   int a=(int)(entry.getKey()>>>32),b=(int)(long)entry.getKey();ArrayList<int[]> list=entry.getValue();boolean[] used=new boolean[list.size()];
   Map<Integer,ArrayList<Integer>> locations=new HashMap<>();for(int j=0;j<list.size();j++)locations.computeIfAbsent(list.get(j)[0],v->new ArrayList<>()).add(j);
   for(int s=0;s<list.size();s++)if(!used[s]){ArrayList<Integer> group=new ArrayList<>();group.add(s);used[s]=true;
    for(int h=0;h<group.size();h++){int at=list.get(group.get(h))[0],px=at%W,py=at/W;for(int dy=-2;dy<=2;dy++)for(int dx=-2;dx<=2;dx++){if(px+dx<0||px+dx>=W||py+dy<0||py+dy>=H)continue;ArrayList<Integer> near=locations.get((py+dy)*W+px+dx);if(near!=null)for(int j:near)if(!used[j]){used[j]=true;group.add(j);}}}
    int[] best=list.get(s);int score=-1;
    for(int j:group){int[] p=list.get(j);int at=p[0],d=clearance[(at/W)/CELL*COLS+(at%W)/CELL];if(d>score){score=d;best=p;}}
    int ac=best[0],bc=best[1],ax=ac%W,ay=ac/W,bx=bc%W,by=bc/W,cx=(ax+bx)/2,cy=(ay+by)/2;
    int[][] pa=regionPath(a,ac,parents,pixels),pb=regionPath(b,bc,parents,pixels);
    doors.add(new Door(doors.size(),a,b,cx,cy,width(cx,cy),ax,ay,bx,by,pa,pb));
   }
  }
  try{
   JSONObject root=new JSONObject().put("world",new JSONArray().put(W).put(H)).put("origin","bottom_left").put("coordinates","x right, y up; all positions are floor points; no automatic pathfinder");
   JSONArray rs=new JSONArray();for(Room r:rooms)rs.put(new JSONArray().put(r.id).put(r.x).put(r.y).put(r.area));root.put("rooms_id_x_y_area",rs);
   JSONArray ds=new JSONArray();for(Door d:doors)ds.put(new JSONArray().put(d.id).put(d.a).put(d.b).put(d.x).put(d.y).put(d.width).put(Math.round(d.length)));root.put("doors_id_roomA_roomB_x_y_width_linkLength",ds);
   // Door links already contain their lengths. An all-pairs distance matrix
   // grows quadratically on detailed terrain and repeats derivable facts in
   // every API request; dynamic route candidates retain exact path lengths.
   root.put("distance_costs","Sum door linkLength along a route; current candidate routes include their total length.");
   description="STATIC_NAVIGATION (collision-derived regions; door widths are conservative samples, routes are door IDs):\n"+root;
  }catch(JSONException e){throw new IllegalStateException(e);}
  buildMs=(System.nanoTime()-started)/1000000;
 }
 private int neighbor(int c,int k){int xx=c%COLS+DX[k],yy=c/COLS+DY[k];return xx>=0&&yy>=0&&xx<COLS&&yy<ROWS?yy*COLS+xx:-1;}
 public boolean wall(int x,int y){return x<0||y<0||x>=W||y>=H||walls[y*W+x]!=0;}
 public boolean clear(int ax,int ay,int bx,int by){int n=Math.max(Math.abs(bx-ax),Math.abs(by-ay));for(int i=0;i<=n;i++){int xx=n==0?ax:ax+(int)Math.round((bx-ax)*i/(double)n),yy=n==0?ay:ay+(int)Math.round((by-ay)*i/(double)n);if(wall(xx,yy))return false;}return true;}
 public int roomAt(int px,int py){return wall(px,py)?-1:pixelRoom[py*W+px];}
 private int width(int px,int py){for(int r=1;r<=100;r++)for(int k=0;k<16;k++){double t=k*Math.PI/8;if(wall(px+(int)Math.round(Math.cos(t)*r),py+(int)Math.round(Math.sin(t)*r)))return Math.max(1,2*(r-1));}return 200;}
 private int[][] regionPath(int room,int target,int[] parent,int[] queue){Arrays.fill(parent,-1);Room r=rooms.get(room);int start=r.y*W+r.x;int h=0,n=0;queue[n++]=start;parent[start]=start;while(h<n&&parent[target]<0){int at=queue[h++],px=at%W,py=at/W;for(int k=0;k<4;k++){int xx=px+DX[k],yy=py+DY[k];if(wall(xx,yy))continue;int q=yy*W+xx;if(pixelRoom[q]!=room||parent[q]>=0)continue;parent[q]=at;queue[n++]=q;}}if(parent[target]<0)throw new IllegalArgumentException("Disconnected portal region");ArrayList<int[]> raw=new ArrayList<>();for(int at=target;;at=parent[at]){raw.add(new int[]{at%W,at/W});if(at==start)break;}Collections.reverse(raw);ArrayList<int[]> path=new ArrayList<>();int at=0;path.add(raw.get(0));while(at<raw.size()-1){int next=raw.size()-1;while(next>at+1&&!clear(raw.get(at)[0],raw.get(at)[1],raw.get(next)[0],raw.get(next)[1]))next--;path.add(raw.get(next));at=next;}return path.toArray(new int[0][]);}
 public int[] legalDoors(int room){return doors.stream().filter(d->d.a==room||d.b==room).mapToInt(d->d.id).toArray();}
 private double[] roomDistances(int source,Set<Integer> banned){double[] d=new double[rooms.size()];Arrays.fill(d,Double.POSITIVE_INFINITY);if(source<0||source>=d.length)return d;d[source]=0;boolean[] used=new boolean[d.length];for(int turn=0;turn<d.length;turn++){int at=-1;for(int i=0;i<d.length;i++)if(!used[i]&&(at<0||d[i]<d[at]))at=i;if(at<0||!Double.isFinite(d[at]))break;used[at]=true;for(Door door:doors){if(banned!=null&&banned.contains(door.id))continue;int next=door.other(at);if(next>=0&&d[next]>d[at]+door.length)d[next]=d[at]+door.length;}}return d;}
 private Route shortest(int from,int to,Set<Integer> banned){
  if(from<0||to<0)return null;double[] dist=new double[rooms.size()];Arrays.fill(dist,Double.POSITIVE_INFINITY);int[] parent=new int[rooms.size()],via=new int[rooms.size()];Arrays.fill(parent,-1);boolean[] used=new boolean[rooms.size()];dist[from]=0;
  for(int turn=0;turn<rooms.size();turn++){int at=-1;for(int i=0;i<dist.length;i++)if(!used[i]&&(at<0||dist[i]<dist[at]))at=i;if(at<0||!Double.isFinite(dist[at]))break;if(at==to)break;used[at]=true;for(Door door:doors)if(!banned.contains(door.id)){int next=door.other(at);if(next>=0&&!used[next]&&dist[next]>dist[at]+door.length){dist[next]=dist[at]+door.length;parent[next]=at;via[next]=door.id;}}}
  if(!Double.isFinite(dist[to]))return null;ArrayList<Integer> ds=new ArrayList<>(),rs=new ArrayList<>();for(int r=to;;r=parent[r]){rs.add(r);if(r==from)break;if(parent[r]<0)return null;ds.add(via[r]);}Collections.reverse(ds);Collections.reverse(rs);return new Route(ds.stream().mapToInt(v->v).toArray(),rs.stream().mapToInt(v->v).toArray(),dist[to]);
 }
 public List<Route> routes(int from,int to,int limit){
  ArrayList<Route> out=new ArrayList<>();Route first=shortest(from,to,Collections.emptySet());if(first==null)return out;out.add(first);ArrayList<Route> alternatives=new ArrayList<>();Set<String> seen=new HashSet<>();seen.add(Arrays.toString(first.doors));
  for(int door:first.doors){Route r=shortest(from,to,Collections.singleton(door));if(r!=null&&seen.add(Arrays.toString(r.doors)))alternatives.add(r);}
  alternatives.sort(Comparator.comparingDouble(r->r.length));for(Route r:alternatives)if(out.size()<limit)out.add(r);return out;
 }
 public String validate(int from,ModelAction action){
  if(action.route()==null)return null;if("stop".equals(action.intent))return action.route().length==0?null:"stop route must be empty";
  int room=from;for(int id:action.route()){if(id<0||id>=doors.size())return "unknown door "+id;int next=doors.get(id).other(room);if(next<0)return "door "+id+" not adjacent to room "+room;room=next;}
  short[] points=action.points();if(points[0]<0)return "active action requires destination";
  // Regions are visibility partitions, not collision boundaries. A floor
  // destination may cross a region boundary if the final door can see it.
  for(int i=0;i<5;i++)if(points[i]>=0){if(wall(points[i],points[i+5]))return "destination wall";int ax=action.route().length==0?rooms.get(room).x:doors.get(action.route()[action.route().length-1]).x,ay=action.route().length==0?rooms.get(room).y:doors.get(action.route()[action.route().length-1]).y;if(!clear(ax,ay,points[i],points[i+5]))return "destination not visible from final door";}
  return null;
 }
 public String validateFrom(int from,int x,int y,ModelAction action){if(action.route()!=null&&action.route().length==0&&!"stop".equals(action.intent)){short[] points=action.points();boolean visible=points[0]>=0;for(int i=0;i<5;i++)if(points[i]>=0)visible&=clear(x,y,points[i],points[i+5]);if(visible)return null;}return validate(from,action);}
 /** Fixed geometry for the exact model-selected portal sequence. */
 public List<int[]> waypoints(int from,int[] route){ArrayList<int[]> out=new ArrayList<>();int room=from;Room r=rooms.get(room);out.add(new int[]{r.x,r.y,-1});for(int id:route){Door d=doors.get(id);boolean forward=d.a==room;int[][] enter=forward?d.pathA:d.pathB,leave=forward?d.pathB:d.pathA;for(int[] p:enter)out.add(new int[]{p[0],p[1],-1});out.add(new int[]{d.x,d.y,id});for(int k=leave.length-1;k>=0;k--)out.add(new int[]{leave[k][0],leave[k][1],id});room=d.other(room);}return out;}
}
