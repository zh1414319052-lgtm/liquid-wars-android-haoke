include $(CLEAR_VARS)

MY_LOCAL_PATH := $(LOCAL_PATH)/application

LOCAL_MODULE := application

MY_LOCAL_SRC_FILES := $(wildcard $(MY_LOCAL_PATH)/*.cpp)
MY_LOCAL_SRC_FILES := $(filter-out $(MY_LOCAL_PATH)/visual_texture.cpp,$(MY_LOCAL_SRC_FILES))
MY_LOCAL_SRC_FILES := $(filter-out $(MY_LOCAL_PATH)/ai_tactics.cpp,$(MY_LOCAL_SRC_FILES))
MY_LOCAL_SRC_FILES := $(filter-out $(MY_LOCAL_PATH)/graphics.cpp,$(MY_LOCAL_SRC_FILES))
LOCAL_SRC_FILES := $(subst cpp/, , $(MY_LOCAL_SRC_FILES))

LOCAL_STATIC_LIBRARIES := aclib visual_quality ai_strategy visual_render

LOCAL_CFLAGS := -Wall -Werror
# Strict IEEE optimization removes debug-build instruction overhead, not simulation steps.
LOCAL_CPPFLAGS := -O2 -fno-fast-math -ffp-contract=off

include $(BUILD_STATIC_LIBRARY)

# Rendering remains separately optimized and independently batched.
include $(CLEAR_VARS)
LOCAL_MODULE := visual_render
LOCAL_SRC_FILES := application/graphics.cpp
LOCAL_STATIC_LIBRARIES := aclib
LOCAL_CPPFLAGS := -O2 -fno-fast-math -ffp-contract=off -Wall -Werror
include $(BUILD_STATIC_LIBRARY)

include $(CLEAR_VARS)
LOCAL_MODULE := ai_strategy
LOCAL_SRC_FILES := application/ai_tactics.cpp
LOCAL_CPPFLAGS := -O2 -Wall -Werror
include $(BUILD_STATIC_LIBRARY)

# CPU visual resampling has its own optimized module.
include $(CLEAR_VARS)
LOCAL_MODULE := visual_quality
LOCAL_SRC_FILES := application/visual_texture.cpp
LOCAL_CPPFLAGS := -O2 -Wall -Werror
include $(BUILD_STATIC_LIBRARY)
