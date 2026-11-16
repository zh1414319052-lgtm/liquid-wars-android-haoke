extends SceneTree
func _init():
	for key in ["application/boot_splash/show_image", "application/boot_splash/bg_color"]:
		assert(ProjectSettings.has_setting(key), key)
		print(key, "=", ProjectSettings.get_setting(key))
	quit()
