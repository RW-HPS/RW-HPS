//Netty Version
val nettyVersion = "4.1.85.Final"

/**
 * Fuck implementation
 */
dependencies {
	api("org.jetbrains.kotlin:kotlin-stdlib:1.7.20")

	implementation(project(":TimeTaskQuartz"))
	implementation(project(":ASM-Framework"))

	//implementation("com.github.minxyzgo.rw-injection:core:077d92e08c")
	//compileOnly("com.github.minxyzgo.rw-injection:source:master-SNAPSHOT")

	api("io.netty:netty-buffer:$nettyVersion")
	api("io.netty:netty-codec:$nettyVersion")
	api("io.netty:netty-codec-http:$nettyVersion")
	api("io.netty:netty-handler:$nettyVersion")
	api("io.netty:netty-transport:$nettyVersion")
	api("io.netty:netty-transport-native-epoll:$nettyVersion:linux-aarch_64")
	api("io.netty:netty-transport-native-epoll:$nettyVersion:linux-x86_64")

	//api fileTree(dir:"libs",include:["ChainMarket-23fc7f989f.jar"])
	//api fileTree(dir:"libs",include:["ChainMarket-23fc7f989f.jar"])
	implementation(fileTree(mapOf("dir" to "libs", "include" to "game-lib.jar")))
	implementation(fileTree(mapOf("dir" to "libs", "include" to "slick.jar")))
	implementation(fileTree(mapOf("dir" to "libs", "include" to "lwjgl.jar")))
	//implementation(fileTree(mapOf("dir" to "libs", "include" to "*.jar")))

	api("com.github.deng-rui:RUDP:2.0.0")
	//implementation(fileTree(mapOf("dir" to "libs", "include" to "RUDP.jar")))
	//api("com.github.jmecn:TMXLoader:v0.2")

	// Json 解析
	// 我建议使用 RW-HPS Json 方法 而不是直接使用依赖
	api("com.google.code.gson:gson:2.10")
	api("org.json:json:20220924")

	api("org.apache.commons:commons-compress:1.21")
	implementation("org.tukaani:xz:1.9")

	api("com.squareup.okhttp3:okhttp:4.10.0") {
		exclude(group = "org.jetbrains.kotlin")
	}
	api("com.vdurmont:emoji-java:5.1.1") {
		exclude(group = "org.json")
	}


	//implementation("org.apache.maven:maven-core:3.8.6")
	implementation("org.apache.maven:maven-project:2.2.1")
	implementation("org.apache.maven:maven-model:3.8.6")
	implementation("org.lionsoul:ip2region:1.7.2")

	implementation("org.jline:jline-reader:3.21.0")
	implementation("org.fusesource.jansi:jansi:2.4.0")

	api("it.unimi.dsi:fastutil-core:8.5.11")

	testApi("org.junit.jupiter:junit-jupiter-engine:5.9.0")
}

tasks.test {
	useJUnitPlatform()
}

publishing {
	publications {
		create<MavenPublication>("maven") {
			groupId = "com.github.RW-HPS"
			artifactId = "Server-Core"
			description = "Dedicated to Rusted Warfare(RustedWarfare) High Performance Server"
			version = "1.0.0"

			from(components.getByName("java"))

			pom {
				scm {
					url.set("https://github.com/RW-HPS/RW-HPS")
					connection.set("scm:https://github.com/RW-HPS/RW-HPS.git")
					developerConnection.set("scm:git@github.com:RW-HPS/RW-HPS.git")
				}

				licenses {
					license {
						name.set("GNU AGPLv3")
						url.set("https://github.com/RW-HPS/RW-HPS/blob/master/LICENSE")
					}
				}

				developers {
					developer {
						id.set("RW-HPS")
						name.set("RW-HPS Technologies")
					}
				}

			}

			pom.withXml {
				val root = asNode()
				root.appendNode("description", project.description)
				root.appendNode("name", project.name)
				root.appendNode("url", "https://github.com/RW-HPS/RW-HPS")
			}
		}
	}
}