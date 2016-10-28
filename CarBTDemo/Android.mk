LOCAL_PATH:= $(call my-dir)
include $(CLEAR_VARS)


LOCAL_MODULE_TAGS := optional

LOCAL_SRC_FILES := $(call all-subdir-java-files)

LOCAL_JAVA_LIBRARIES := mediatek-common
LOCAL_JAVA_LIBRARIES += mediatek-framework
LOCAL_JAVA_LIBRARIES += javax.obex

LOCAL_STATIC_JAVA_LIBRARIES := android.bluetooth.client.pbap
LOCAL_STATIC_JAVA_LIBRARIES += com.android.vcard
LOCAL_STATIC_JAVA_LIBRARIES += android-support-v13
LOCAL_STATIC_JAVA_LIBRARIES += android-support-v4
LOCAL_STATIC_JAVA_LIBRARIES += jsr305

LOCAL_PACKAGE_NAME := CarBTDemo
LOCAL_CERTIFICATE := platform

include $(BUILD_PACKAGE)

########################
#include $(call all-makefiles-under,$(LOCAL_PATH))
