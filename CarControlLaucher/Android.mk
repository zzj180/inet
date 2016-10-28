LOCAL_PATH:= $(call my-dir)
include $(CLEAR_VARS)

LOCAL_MODULE_TAGS := optional

LOCAL_SHARED_JAVA_LIBRARIES := libjavacore

LOCAL_SRC_FILES := $(call all-java-files-under, src)
LOCAL_SRC_FILES += \
    src/cn/colink/serialport/service/ISerialPortCallback.aidl \
    src/cn/colink/serialport/service/ISerialPortService.aidl \

LOCAL_PACKAGE_NAME := CarControlLaucher
LOCAL_CERTIFICATE := platform

LOCAL_STATIC_JAVA_LIBRARIES := android-support-v4-my zxing baiduLocation

LOCAL_SRC_FILES := $(call all-java-files-under, src)

LOCAL_RESOURCE_DIR += $(LOCAL_PATH)/res

LOCAL_OVERRIDES_PACKAGES := Provision

include $(BUILD_PACKAGE)

include $(CLEAR_VARS)
LOCAL_PREBUILT_STATIC_JAVA_LIBRARIES :=android-support-v4-my:libs/android-support-v4.jar \
									   zxing:libs/zxing.jar \
									   baiduLocation:libs/BaiduLBS_Android.jar \

include $(BUILD_MULTI_PREBUILT)
# Use the folloing include to make our test apk.
include $(call all-makefiles-under,$(LOCAL_PATH))
