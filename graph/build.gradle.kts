plugins { id("trace.library") }
dependencies { implementation(project(":store")); testImplementation(platform(libs.junit.bom)); testImplementation(libs.junit.jupiter); testRuntimeOnly("org.junit.platform:junit-platform-launcher") }

