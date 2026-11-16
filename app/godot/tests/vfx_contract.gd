extends SceneTree
func _initialize() -> void:
 call_deferred("_run")
func _run() -> void:
 var fx := BattleVfx.new()
 root.add_child(fx)
 await process_frame
 var frame := PackedFloat32Array()
 frame.resize(56)
 frame[2]=1;frame[4]=100;frame[5]=100;frame[6]=400;frame[7]=200
 var records := [[8,200,180,0,-180,3,0,1,.5,0], [50,-2000,190,0,-180,12,0,0,1,0], [62,-3002,190,0,-180,12,0,0,1,0], [121,-1000001,190,0,-180,1,0,2,0,0]]
 for i in records.size():
  for k in 10:frame[16+i*10+k]=records[i][k]
 fx.update_frame(frame,16,4,2)
 assert(fx.used>20 and fx.used<fx.LIMIT)
 var paused := fx.buffer.duplicate()
 fx.update_frame(frame,16,4,2)
 assert(fx.buffer==paused,"Paused game time must freeze all VFX")
 frame[2]=2
 fx.update_frame(frame,16,0,0)
 assert(fx.previous.is_empty() and fx.events.is_empty() and fx.bursts.is_empty())
 assert(fx.used==0)
 print("VFX_CONTRACT_PASS: timed health, pooled effects, pause and epoch reset")
 quit()
