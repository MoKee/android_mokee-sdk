# Copyright (C) 2015 The CyanogenMod Project
# Copyright (C) 2017-2019 The LineageOS Project
# Copyright (C) 2015-2019 The MoKee Open Source Project
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
mokee_platform_res := APPS/org.mokee.platform-res_intermediates/aapt

# List of packages used in mokee-api-stubs
mokee_stub_packages := mokee.app:mokee.content:mokee.hardware:mokee.media:mokee.os:mokee.preference:mokee.profiles:mokee.providers:mokee.platform:mokee.power:mokee.util:mokee.weather:mokee.weatherservice:mokee.style:mokee.trust

mokee_framework_module := $(LOCAL_INSTALLED_MODULE)

# Make sure that R.java and Manifest.java are built before we build
# the source for this library.
mokee_framework_res_R_stamp := \
    $(call intermediates-dir-for,APPS,org.mokee.platform-res,,COMMON)/src/R.stamp
LOCAL_ADDITIONAL_DEPENDENCIES := $(mokee_framework_res_R_stamp)

$(mokee_framework_module): | $(dir $(mokee_framework_module))org.mokee.platform-res.apk

mokee_framework_built := $(call java-lib-deps, org.mokee.platform)

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

LOCAL_STATIC_JAVA_LIBRARIES := org.mokee.platform.sdk

include $(BUILD_STATIC_JAVA_LIBRARY)
$(LOCAL_MODULE) : $(built_aar)

# ===========================================================
# Common Droiddoc vars
mokee_platform_docs_src_files := \
    $(call all-java-files-under, $(mokee_sdk_src)) \
    $(call all-html-files-under, $(mokee_sdk_src))

mokee_platform_docs_java_libraries := \
    org.mokee.platform.sdk

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
    services

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
