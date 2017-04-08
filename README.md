#  JAX-RPC Maven Plugin

This plugin can generate Java classes for JAX-RPC web service clients/servers.

Note: JAX-RPC is obsolete framework and if you work with Web Services in Java using JAX-WS and looking for Maven plugin,
you probably want to go here [jaxws-maven-plugin](http://www.mojohaus.org/jaxws-maven-plugin/).
 

## Installation

* Execute `mvn install`

## Usage

This sample shows usage of the plugin.

Content of pom.xml:

```xml
<project xmlns="http://maven.apache.org/POM/4.0.0">
	<modelVersion>4.0.0</modelVersion>
	<groupId>com.company.portal</groupId>
	<artifactId>WSwsdls</artifactId>
	<packaging>jar</packaging>
	<version>1.0.0-SNAPSHOT</version>
	<dependencies>
		<dependency>
			<groupId>com.sun.xml.rpc</groupId>
			<artifactId>jaxrpc-impl</artifactId>
			<version>1.1.3_01</version>
			<scope>provided</scope>
		</dependency>
	</dependencies>
	<build>
		<plugins>
			<plugin>
				<groupId>net.sf.jaxrpc-maven</groupId>
				<artifactId>maven-jaxrpc-plugin</artifactId>
				<version>0.3</version>
				<executions>
					<execution>
						<id>jax-rpc-scoring-client</id>
						<phase>process-resources</phase>
						<goals>
							<goal>wscompile</goal>
						</goals>
					</execution>
				</executions>
				<configuration>
					<config>${project.basedir}/jaxrpc-config.xml</config>
					<operation>gen:client</operation>
					<mapping>${project.build.outputDirectory}/META-INF/jaxrpc-mapping.xml</mapping>
					<nd>${project.build.outputDirectory}/META-INF/wsdl</nd>
					<d>${project.build.directory}/generated-classes/jaxrpc</d>
					<keep>true</keep>
					<verbose>true</verbose>
				</configuration>
			</plugin>
		</plugins>
	</build>
</project>
```

jaxrpc-config.xml:

```xml
<configuration xmlns="http://java.sun.com/xml/ns/jax-rpc/ri/config">
	<wsdl 
		location="src/main/wsdl/queuemanager.wsdl"
		packageName="com.company.queuemanager.generated"/>
</configuration>
```

