# Copyright (C) 2015 The CyanogenMod Project
# Copyright (C) 2015-2017 The MoKee Open Source Project
#
# Licensed under the Apache License, Version 2.0 (the "License");
# you may not use this file except in compliance with the License.
# You may obtain a copy of the License at
#
#      http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License.
LOCAL_PATH := $(call my-dir)

# We have a special case here where we build the library's resources
# independently from its code, so we need to find where the resource
# class source got placed in the course of building the resources.
# Thus, the magic here.
# Also, this module cannot depend directly on the R.java file; if it
# did, the PRIVATE_* vars for R.java wouldn't be guaranteed to be correct.
# Instead, it depends on the R.stamp file, which lists the corresponding
# R.java file as a prerequisite.
mokee_platform_res := APPS/org.mokee.platform-res_intermediates/src

# List of packages used in mokee-api-stubs
mokee_stub_packages := mokee.app:mokee.content:mokee.hardware:mokee.media:mokee.os:mokee.preference:mokee.profiles:mokee.providers:mokee.platform:mokee.power:mokee.util:mokee.weather:mokee.weatherservice:mokee.style:mokee.trust

# The MoKee Platform Framework Library
# ============================================================
include $(CLEAR_VARS)

mokee_sdk_src := sdk/src/java/mokee
mokee_sdk_internal_src := sdk/src/java/org/mokee/internal
library_src := mk/lib/main/java

LOCAL_MODULE := org.mokee.platform
LOCAL_MODULE_TAGS := optional

mokee_sdk_LOCAL_JAVA_LIBRARIES := \
    android-support-annotations \
    android-support-v7-preference \
    android-support-v7-recyclerview \
    android-support-v14-preference

LOCAL_JAVA_LIBRARIES := \
    services \
    org.mokee.hardware \
    $(mokee_sdk_LOCAL_JAVA_LIBRARIES)

LOCAL_SRC_FILES := \
    $(call all-java-files-under, $(mokee_sdk_src)) \
    $(call all-java-files-under, $(mokee_sdk_internal_src)) \
    $(call all-java-files-under, $(library_src))

## READ ME: ########################################################
##
## When updating this list of aidl files, consider if that aidl is
## part of the SDK API.  If it is, also add it to the list below that
## is preprocessed and distributed with the SDK.  This list should
## not contain any aidl files for parcelables, but the one below should
## if you intend for 3rd parties to be able to send those objects
## across process boundaries.
##
## READ ME: ########################################################
LOCAL_SRC_FILES += \
    $(call all-Iaidl-files-under, $(mokee_sdk_src)) \
    $(call all-Iaidl-files-under, $(mokee_sdk_internal_src))

mokee_platform_LOCAL_INTERMEDIATE_SOURCES := \
    $(mokee_platform_res)/mokee/platform/R.java \
    $(mokee_platform_res)/mokee/platform/Manifest.java \
    $(mokee_platform_res)/org/mokee/platform/internal/R.java

LOCAL_INTERMEDIATE_SOURCES := \
    $(mokee_platform_LOCAL_INTERMEDIATE_SOURCES)

# Include aidl files from mokee.app namespace as well as internal src aidl files
LOCAL_AIDL_INCLUDES := $(LOCAL_PATH)/sdk/src/java
LOCAL_AIDL_FLAGS := -n

include $(BUILD_JAVA_LIBRARY)
mokee_framework_module := $(LOCAL_INSTALLED_MODULE)

# Make sure that R.java and Manifest.java are built before we build
# the source for this library.
mokee_framework_res_R_stamp := \
    $(call intermediates-dir-for,APPS,org.mokee.platform-res,,COMMON)/src/R.stamp
LOCAL_ADDITIONAL_DEPENDENCIES := $(mokee_framework_res_R_stamp)

$(mokee_framework_module): | $(dir $(mokee_framework_module))org.mokee.platform-res.apk

mokee_framework_built := $(call java-lib-deps, org.mokee.platform)

# ====  org.mokee.platform.xml lib def  ========================
include $(CLEAR_VARS)

LOCAL_MODULE := org.mokee.platform.xml
LOCAL_MODULE_TAGS := optional

LOCAL_MODULE_CLASS := ETC

# This will install the file in /system/etc/permissions
LOCAL_MODULE_PATH := $(TARGET_OUT_ETC)/permissions

LOCAL_SRC_FILES := $(LOCAL_MODULE)

include $(BUILD_PREBUILT)

# the sdk
# ============================================================
include $(CLEAR_VARS)

LOCAL_MODULE:= org.mokee.platform.sdk
LOCAL_MODULE_TAGS := optional
LOCAL_REQUIRED_MODULES := services

LOCAL_SRC_FILES := \
    $(call all-java-files-under, $(mokee_sdk_src)) \
    $(call all-Iaidl-files-under, $(mokee_sdk_src))

# Included aidl files from mokee.app namespace
LOCAL_AIDL_INCLUDES := $(LOCAL_PATH)/sdk/src/java

mokee_sdk_LOCAL_INTERMEDIATE_SOURCES := \
    $(mokee_platform_res)/mokee/platform/R.java \
    $(mokee_platform_res)/mokee/platform/Manifest.java

LOCAL_INTERMEDIATE_SOURCES := \
    $(mokee_sdk_LOCAL_INTERMEDIATE_SOURCES)

LOCAL_JAVA_LIBRARIES := \
    $(mokee_sdk_LOCAL_JAVA_LIBRARIES)

# Make sure that R.java and Manifest.java are built before we build
# the source for this library.
mokee_framework_res_R_stamp := \
    $(call intermediates-dir-for,APPS,org.mokee.platform-res,,COMMON)/src/R.stamp
LOCAL_ADDITIONAL_DEPENDENCIES := $(mokee_framework_res_R_stamp)
include $(BUILD_STATIC_JAVA_LIBRARY)

# the sdk as an aar for publish, not built as part of full target
# DO NOT LINK AGAINST THIS IN BUILD
# ============================================================
include $(CLEAR_VARS)

LOCAL_MODULE := org.mokee.platform.sdk.aar

LOCAL_JACK_ENABLED := disabled

LOCAL_CONSUMER_PROGUARD_FILE := $(LOCAL_PATH)/sdk/proguard.txt

LOCAL_RESOURCE_DIR := $(addprefix $(LOCAL_PATH)/, sdk/res/res)
LOCAL_MANIFEST_FILE := sdk/AndroidManifest.xml

mokee_sdk_exclude_files := 'mokee/library'
LOCAL_JAR_EXCLUDE_PACKAGES := $(mokee_sdk_exclude_files)
LOCAL_JAR_EXCLUDE_FILES := none

LOCAL_JAVA_LIBRARIES := \
    $(mokee_sdk_LOCAL_JAVA_LIBRARIES)

LOCAL_STATIC_JAVA_LIBRARIES := org.mokee.platform.sdk

include $(BUILD_STATIC_JAVA_LIBRARY)
$(LOCAL_MODULE) : $(built_aar)

# full target for use by platform apps
#
include $(CLEAR_VARS)

LOCAL_MODULE:= org.mokee.platform.internal
LOCAL_MODULE_TAGS := optional
LOCAL_REQUIRED_MODULES := services

LOCAL_SRC_FILES := \
    $(call all-java-files-under, $(mokee_sdk_src)) \
    $(call all-java-files-under, $(mokee_sdk_internal_src)) \
    $(call all-Iaidl-files-under, $(mokee_sdk_src)) \
    $(call all-Iaidl-files-under, $(mokee_sdk_internal_src))

# Included aidl files from mokee.app namespace
LOCAL_AIDL_INCLUDES := $(LOCAL_PATH)/sdk/src/java
LOCAL_AIDL_FLAGS := -n

mokee_sdk_LOCAL_INTERMEDIATE_SOURCES := \
    $(mokee_platform_res)/mokee/platform/R.java \
    $(mokee_platform_res)/mokee/platform/Manifest.java \
    $(mokee_platform_res)/org/mokee/platform/internal/R.java \
    $(mokee_platform_res)/org/mokee/platform/internal/Manifest.java

LOCAL_INTERMEDIATE_SOURCES := \
    $(mokee_sdk_LOCAL_INTERMEDIATE_SOURCES)

LOCAL_JAVA_LIBRARIES := \
    $(mokee_sdk_LOCAL_JAVA_LIBRARIES)

$(full_target): $(mokee_framework_built) $(gen)
include $(BUILD_STATIC_JAVA_LIBRARY)


# ===========================================================
# Common Droiddoc vars
mokee_platform_docs_src_files := \
    $(call all-java-files-under, $(mokee_sdk_src)) \
    $(call all-html-files-under, $(mokee_sdk_src))

mokee_platform_docs_java_libraries := \
    android-support-v4 \
    org.mokee.platform.sdk \
    $(mokee_sdk_LOCAL_JAVA_LIBRARIES)

# SDK version as defined
mokee_platform_docs_SDK_VERSION := 81.0

# release version
mokee_platform_docs_SDK_REL_ID := 9

mokee_platform_docs_LOCAL_MODULE_CLASS := JAVA_LIBRARIES

mokee_platform_docs_LOCAL_DROIDDOC_SOURCE_PATH := \
    $(mokee_platform_docs_src_files)

mokee_platform_docs_LOCAL_ADDITIONAL_JAVA_DIR := \
    $(call intermediates-dir-for,JAVA_LIBRARIES,org.mokee.platform.sdk,,COMMON)

# ====  the api stubs and current.xml ===========================
include $(CLEAR_VARS)

LOCAL_SRC_FILES:= \
    $(mokee_platform_docs_src_files)
LOCAL_INTERMEDIATE_SOURCES:= $(mokee_platform_LOCAL_INTERMEDIATE_SOURCES)
LOCAL_JAVA_LIBRARIES:= $(mokee_platform_docs_java_libraries)
LOCAL_MODULE_CLASS:= $(mokee_platform_docs_LOCAL_MODULE_CLASS)
LOCAL_DROIDDOC_SOURCE_PATH:= $(mokee_platform_docs_LOCAL_DROIDDOC_SOURCE_PATH)
LOCAL_ADDITIONAL_JAVA_DIR:= $(mokee_platform_docs_LOCAL_ADDITIONAL_JAVA_DIR)
LOCAL_ADDITIONAL_DEPENDENCIES:= $(mokee_platform_docs_LOCAL_ADDITIONAL_DEPENDENCIES)

LOCAL_MODULE := mokee-api-stubs

LOCAL_DROIDDOC_CUSTOM_TEMPLATE_DIR:= external/doclava/res/assets/templates-sdk

LOCAL_DROIDDOC_STUB_OUT_DIR := $(TARGET_OUT_COMMON_INTERMEDIATES)/JAVA_LIBRARIES/mokee-sdk_stubs_current_intermediates/src

LOCAL_DROIDDOC_OPTIONS:= \
        -referenceonly \
        -stubpackages $(mokee_stub_packages) \
        -exclude org.mokee.platform.internal \
        -api $(INTERNAL_MOKEE_PLATFORM_API_FILE) \
        -removedApi $(INTERNAL_MOKEE_PLATFORM_REMOVED_API_FILE) \
        -nodocs

LOCAL_UNINSTALLABLE_MODULE := true

include $(BUILD_DROIDDOC)

# $(gen), i.e. framework.aidl, is also needed while building against the current stub.
$(full_target): $(mokee_framework_built) $(gen)
$(INTERNAL_MOKEE_PLATFORM_API_FILE): $(full_target)
$(call dist-for-goals,sdk,$(INTERNAL_MOKEE_PLATFORM_API_FILE))


# Documentation
# ===========================================================
include $(CLEAR_VARS)

LOCAL_MODULE := org.mokee.platform.sdk
LOCAL_INTERMEDIATE_SOURCES:= $(mokee_platform_LOCAL_INTERMEDIATE_SOURCES)
LOCAL_MODULE_CLASS := JAVA_LIBRARIES
LOCAL_MODULE_TAGS := optional

LOCAL_SRC_FILES := $(mokee_platform_docs_src_files)
LOCAL_ADDITONAL_JAVA_DIR := $(mokee_platform_docs_LOCAL_ADDITIONAL_JAVA_DIR)

LOCAL_IS_HOST_MODULE := false
LOCAL_ADDITIONAL_DEPENDENCIES := \
    services \
    org.mokee.hardware

LOCAL_JAVA_LIBRARIES := $(mokee_platform_docs_java_libraries)

LOCAL_DROIDDOC_OPTIONS := \
        -android \
        -offlinemode \
        -exclude org.mokee.platform.internal \
        -hidePackage org.mokee.platform.internal \
        -hdf android.whichdoc offline \
        -hdf sdk.version $(mokee_platform_docs_docs_SDK_VERSION) \
        -hdf sdk.rel.id $(mokee_platform_docs_docs_SDK_REL_ID) \
        -hdf sdk.preview 0 \
        -since $(MOKEE_SRC_API_DIR)/1.txt 1 \
        -since $(MOKEE_SRC_API_DIR)/2.txt 2 \
        -since $(MOKEE_SRC_API_DIR)/3.txt 3 \
        -since $(MOKEE_SRC_API_DIR)/4.txt 4 \
        -since $(MOKEE_SRC_API_DIR)/5.txt 5 \
        -since $(MOKEE_SRC_API_DIR)/6.txt 6 \
        -since $(MOKEE_SRC_API_DIR)/7.txt 7 \
        -since $(MOKEE_SRC_API_DIR)/8.txt 8 \
        -since $(MOKEE_SRC_API_DIR)/9.txt 9

$(full_target): $(mokee_framework_built) $(gen)
include $(BUILD_DROIDDOC)

include $(call first-makefiles-under,$(LOCAL_PATH))

# Cleanup temp vars
# ===========================================================
mokee_platform_docs_src_files :=
mokee_platform_docs_java_libraries :=
mokee_platform_docs_LOCAL_ADDITIONAL_JAVA_DIR :=
