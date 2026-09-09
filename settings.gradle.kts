rootProject.name = "KmpStreamTv"

pluginManagement {
    repositories {
        google {
            mavenContent {
                includeGroupAndSubgroups("androidx")
                includeGroupAndSubgroups("com.android")
                includeGroupAndSubgroups("com.google")
            }
        }
        mavenCentral()
        gradlePluginPortal()
    }
}

dependencyResolutionManagement {
    repositories {
        google {
            mavenContent {
                includeGroupAndSubgroups("androidx")
                includeGroupAndSubgroups("com.android")
                includeGroupAndSubgroups("com.google")
            }
        }
        mavenCentral()
    }
}

include(":androidApp")
include(":shared")

// Android playback is developed as a reusable sibling project. The composite build substitutes
// com.congnguyencn:stream-player without requiring a local Maven publish step.
val streamPlayerDir = file("../android_stream_player")
require(streamPlayerDir.isDirectory) {
    "Expected android_stream_player next to kmp-stream-tv at ${streamPlayerDir.absolutePath}"
}
includeBuild(streamPlayerDir)
