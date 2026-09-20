plugins { alias(libs.plugins.kotlin.jvm) }

kotlin { jvmToolchain(21) }

dependencies {
    implementation(project(":core:model"))
    implementation(project(":core:algorithm"))
    implementation(libs.kotlinx.coroutines.core)
    testImplementation(kotlin("test"))
}
