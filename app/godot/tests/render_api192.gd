extends SceneTree
func _initialize():
    for method in ClassDB.class_get_method_list("RenderingServer"):
        if "time" in method.name or "frame" in method.name:
            print(method.name, " ", method.args)
    quit()
