import org.gradle.api.Project
import org.gradle.api.plugins.JavaPluginConvention
import org.gradle.api.publish.PublishingExtension
import org.gradle.api.publish.maven.MavenPom
import org.gradle.api.publish.maven.MavenPublication
import org.gradle.api.tasks.bundling.Jar
import org.gradle.kotlin.dsl.*
import org.gradle.plugins.signing.SigningExtension
import java.net.URI

object Deployment {
    val user = System.getenv("SONATYPE_USERNAME")
    val password = System.getenv("SONATYPE_PASSWORD")
    val githubUser = System.getenv("GITHUB_MAVEN_USERNAME")
    val githubPassword = System.getenv("GITHUB_TOKEN")
    var releaseMode: String? = null
    var versionSuffix: String? = null
    var deployUrl: String? = null

    val snapshotDeployUrl = System.getenv("SONATYPE_SNAPSHOTS_URL")
        ?: "https://oss.sonatype.org/content/repositories/snapshots/"
    val releaseDeployUrl = System.getenv("SONATYPE_RELEASES_URL")
        ?: "https://oss.sonatype.org/service/local/staging/deploy/maven2/"
    val githubDeployUrl = "https://maven.pkg.github.com/Malinskiy/adam"

    fun initialize(project: Project) {
        val releaseMode: String? by project
        val versionSuffix = when (releaseMode) {
            "RELEASE" -> ""
            else -> "-SNAPSHOT"
        }

        Deployment.releaseMode = releaseMode
        Deployment.versionSuffix = versionSuffix
        Deployment.deployUrl = when (releaseMode) {
            "RELEASE" -> Deployment.releaseDeployUrl
            else -> Deployment.snapshotDeployUrl
        }

        initializePublishing(project)
        initializeSigning(project)
    }

    private fun initializePublishing(project: Project) {
        project.version = Versions.adam + versionSuffix

        project.plugins.apply("maven-publish")

        val javaPlugin = project.the(JavaPluginConvention::class)

        val sourcesJar by project.tasks.creating(Jar::class) {
            classifier = "sources"
            from(javaPlugin.sourceSets["main"].allSource)
        }
        val javadocJar by project.tasks.creating(Jar::class) {
            classifier = "javadoc"
            from(javaPlugin.docsDir)
            dependsOn("javadoc")
        }

        project.configure<PublishingExtension> {
            publications {
                create("default", MavenPublication::class.java) {
                    Deployment.customizePom(project, pom)
                    from(project.components["java"])
                    artifact(sourcesJar)
                    artifact(javadocJar)
                }
            }
            repositories {
                maven {
                    name = "Local"
                    setUrl("${project.rootDir}/build/repository")
                }
                maven {
                    name = "OSSHR"
                    credentials {
                        username = Deployment.user
                        password = Deployment.password
                    }
                    url = URI.create(Deployment.deployUrl)
                }
                maven {
                    name = "GitHub"
                    credentials {
                        username = Deployment.githubUser
                        password = Deployment.githubPassword
                    }
                    url = URI.create(Deployment.githubDeployUrl)
                }
            }
        }
    }

    private fun initializeSigning(project: Project) {
        val passphrase = System.getenv("GPG_PASSPHRASE")
        passphrase?.let {
            project.plugins.apply("signing")

            val publishing = project.the(PublishingExtension::class)
            project.configure<SigningExtension> {
                sign(publishing.publications.getByName("default"))
            }

            project.extra.set("signing.keyId", "1131CBA5")
            project.extra.set("signing.password", passphrase)
            project.extra.set("signing.secretKeyRingFile", "${project.rootProject.rootDir}/.buildsystem/secring.gpg")
        }
    }

    fun customizePom(project: Project, pom: MavenPom?) {
        pom?.apply {
            name.set(project.name)
            url.set("https://github.com/Malinskiy/adam")
            description.set("Android Debug Bridge helper")

            licenses {
                license {
                    name.set("The Apache License, Version 2.0")
                    url.set("http://www.apache.org/licenses/LICENSE-2.0.txt")
                }
            }

            developers {
                developer {
                    id.set("Malinskiy")
                    name.set("Anton Malinskiy")
                    email.set("anton@malinskiy.com")
                }
            }

            scm {
                url.set("https://github.com/Malinskiy/adam.git")
                connection.set("scm:git:ssh://github.com/Malinskiy/adam")
                developerConnection.set("scm:git:ssh://github.com/Malinskiy/adam")
            }
        }
    }
}
