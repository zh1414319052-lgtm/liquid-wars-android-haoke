// SPDX-License-Identifier: GPL-3.0-or-later
// Generated from art/source/catalog-1.72.json. IDs are NOT display positions.
package com.xenris.liquidwarsos;
public final class MapCatalog {
    private MapCatalog() {}
    public static final int COUNT=59;
    public static final int LARGE_COUNT=10;
    public static int largeIdAt(int position) {if(position<0||position>=LARGE_COUNT)throw new IllegalArgumentException("Large map selection");return 50+position;}
    private static final int[] IDS={1,2,3,4,5,6,7,8,9,10,11,12,13,14,15,16,17,18,19,20,21,22,23,24,25,26,27,28,29,30,31,32,33,34,35,36,37,38,39,40,41,42,43,44,45,46,47,48,49,50,51,52,53,54,55,56,57,58,59};
    public static int width(int id) {return id>=50?1600:800;}
    public static int height(int id) {return id>=50?960:480;}
    public static boolean validSelection(int position) {return position>=0&&position<=COUNT;}
    public static int mapIdForSelection(int position) {
        if(!validSelection(position))throw new IllegalArgumentException("Map selection");
        return position==0?-1:IDS[position-1];
    }
    public static int selectionForMapId(int id) {for(int i=0;i<COUNT;i++)if(IDS[i]==id)return i+1;return 0;}
    public static boolean validMapChoice(int id) {return id==-1||selectionForMapId(id)>0;}
}
