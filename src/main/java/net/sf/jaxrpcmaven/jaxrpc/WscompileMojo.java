package net.sf.jaxrpcmaven.jaxrpc;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import org.apache.commons.lang.SystemUtils;
import org.apache.maven.artifact.DependencyResolutionRequiredException;
import org.apache.maven.execution.MavenSession;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.Component;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.plugins.annotations.ResolutionScope;
import org.apache.maven.project.MavenProject;
import org.apache.maven.toolchain.Toolchain;
import org.apache.maven.toolchain.ToolchainManager;
import org.codehaus.plexus.util.cli.CommandLineException;
import org.codehaus.plexus.util.cli.CommandLineUtils;
import org.codehaus.plexus.util.cli.Commandline;
import org.codehaus.plexus.util.cli.StreamConsumer;

import com.sun.xml.rpc.tools.wscompile.CompileTool;

/**
 * A wscompile mojo for maven 2.
 *
 * See
 * http://download.oracle.com/docs/cd/E17802_01/webservices/webservices/docs/1.5/jaxrpc/jaxrpc-tools.html
 * for detailed manual.
 * 
 * @author <a href="ludek.h@gmail.com">Ludek Hlavacek</a>
 * @describe wscompile mojo
 */
@Mojo(requiresDependencyResolution = ResolutionScope.COMPILE_PLUS_RUNTIME, requiresProject = true, defaultPhase = LifecyclePhase.PROCESS_CLASSES, name = "wscompile")
public class WscompileMojo extends AbstractMojo {

    /**
     * The maven project.
     * 
     */
    @Parameter(defaultValue = "${project}", readonly = true, required = true)
    private MavenProject project;

    /**
     * operation
     */
    @Parameter(required = true)
    private String operation;

    /**
     * enable the given features
     *
     */
    @Parameter
    private String features;

    /**
     * specify a HTTP proxy server
     *
     */
    @Parameter
    private ProxyConfiguration httpProxy;

    /**
     * output messages about what the compiler is doing
     *
     */
    @Parameter(defaultValue = "false")
    private boolean verbose;

    /**
     * keep generated files
     *
     */
    @Parameter(defaultValue = "false")
    private boolean keep;

    /**
     * generate debugging info
     *
     */
    @Parameter(defaultValue = "false")
    private boolean debug;

    /**
     * optimize generated code
     *
     */
    @Parameter(defaultValue = "false")
    private boolean optimize;

    /**
     * generate a J2EE mapping.xml file
     * 
     */
    @Parameter
    private File mapping;

    /**
     * write the internal model to the given file
     * 
     */
    @Parameter
    private File model;

    /**
     * specify where to place non-class generated files
     * 
     */
    @Parameter
    private File nd;

    /**
     * specify where to place generated source files
     *
     */
    @Parameter(defaultValue = "${project.build.directory}/generated-sources/jaxrpc")
    private File s;
    /**
     * Should be generated source files added to compile path?
     *
     */
    @Parameter(defaultValue = "true")
    private boolean addSources;

    /**
     * specify where to place generated output files
     *
     */
    @Parameter(defaultValue = "${project.build.outputDirectory}")
    private File d;

    /**
     * Generate code for the specified JAX-RPC SI version. Supported versions
     * are: 1.0.1, 1.0.3, and 1.1 (default).
     *
     */
    @Parameter
    private String source;

    /**
     * configuration-file
     *
     */
    @Parameter(required = true)
    private String config;

    /**
     * The current build session instance. This is used for toolchain manager
     * API calls.
     */
    @Parameter(defaultValue = "${session}", readonly = true, required = true)
    private MavenSession session;

    @Component
    private ToolchainManager tcManager;

    private void addToolsToCL() {
	try {
	    URL url = new URL("jar:file://" + findToolsJar() + "!/");
	    ClassLoader cl = new URLClassLoader(new URL[] { url }, getClass().getClassLoader());
	    Thread.currentThread().setContextClassLoader(cl);
	} catch (MalformedURLException e) {
	    getLog().warn("Failed to add tools.jar to classpath");
	}
    }

    public void execute() throws MojoExecutionException, MojoFailureException {

	List<String> args = new ArrayList<String>();

	args.add("-" + operation);

	args.add("-cp");
	args.add(getCp());

	if (features != null) {
	    args.add("-features:" + features);
	}

	if (keep) {
	    args.add("-keep");
	}

	if (debug) {
	    args.add("-g");
	}
	if (optimize) {
	    args.add("-O");
	}
	if ((httpProxy != null) && (httpProxy.getHost() != null)) {
	    args.add("-httpproxy:" + httpProxy.getHost() + ":" + httpProxy.getPort());
	}

	if (mapping != null) {
	    args.add("-mapping");
	    args.add(mapping.getAbsolutePath());
	}
	if (model != null) {
	    args.add("-model");
	    args.add(model.getAbsolutePath());
	}
	if (nd != null) {
	    args.add("-nd");
	    args.add(nd.getAbsolutePath());
	    nd.mkdirs();
	}
	if (s != null) {
	    args.add("-s");
	    args.add(s.getAbsolutePath());
	    s.mkdirs();
	    if (addSources) {
		project.addCompileSourceRoot(s.getAbsolutePath());
	    }
	}
	if (d != null) {
	    args.add("-d");
	    args.add(d.getAbsolutePath());
	    d.mkdirs();
	}
	if (source != null) {
	    args.add("-source");
	    args.add(source);
	}
	if (verbose) {
	    args.add("-verbose");
	}

	args.add(config);

	String[] strArgs = args.toArray(new String[args.size()]);
	getLog().info(Arrays.toString(strArgs));

	addToolsToCL();

	boolean fork = false;
	String javaTool = null;
	Toolchain jdkToolChain = tcManager.getToolchainFromBuildContext("jdk", session);
	if (jdkToolChain != null) {
	    javaTool = jdkToolChain.findTool("java");
	    if (javaTool != null) {
		fork = true;
	    }
	}

	if (!fork) {
	    CompileTool tool = new CompileTool(System.out, "wscompile");
	    boolean result = tool.run(strArgs);
	    if (!result) {
		throw new MojoFailureException("Wscompile failed");
	    }
	    getLog().info("Wscompile succeeded");
	} else {
	    List<String> commandLineArgs = new ArrayList<String>();
	    commandLineArgs.add("-cp");
	    commandLineArgs.add(getCp());
	    commandLineArgs.add(com.sun.xml.rpc.tools.wscompile.Main.class.getCanonicalName());
	    commandLineArgs.addAll(Arrays.asList(strArgs));
	    Commandline cli = new Commandline();
	    cli.setWorkingDirectory(session.getCurrentProject().getBuild().getOutputDirectory());
	    cli.setExecutable(javaTool);
	    cli.addArguments(commandLineArgs.toArray(new String[0]));

	    getLog().debug("Executing in forked mode with [" + cli.toString() + "]");

	    StreamConsumer out=new StreamConsumer() {
	        
	        public void consumeLine(String line) {
	    		System.out.println(line);
	        }
	    };
	    
	    StreamConsumer err=new StreamConsumer() {
	        
	        public void consumeLine(String line) {
	    		System.err.println(line);
	        }
	    };
	    
	    
	    //CommandLineUtils.StringStreamConsumer out = new CommandLineUtils.StringStreamConsumer();
	    //CommandLineUtils.StringStreamConsumer err = new CommandLineUtils.StringStreamConsumer();
	    try {
		int returnCode = CommandLineUtils.executeCommandLine(cli, out, err);
		//System.out.print(out.getOutput());
		//System.err.print(err.getOutput());
		if (returnCode != 0) {
		    throw new MojoFailureException("Wscompile failed");
		}
		getLog().info("Wscompile succeeded");
	    } catch (CommandLineException e) {
		throw new MojoFailureException("Wscompile failed", e);
	    }

	}
    }

    /**
     * Returns the an isolated classloader.
     *
     * @return cpasspath list
     * @noinspection unchecked
     */
    private String getCp() {

	Set<Comparable<String>> cp = new HashSet<Comparable<String>>();

	try {
	    List<String> classpathElements = project.getCompileClasspathElements();
	    for (int i = 0; i < classpathElements.size(); i++) {
		// getLog().info("CP: "+classpathElements.get(i));
		cp.add(classpathElements.get(i));
	    }
	} catch (DependencyResolutionRequiredException ex) {
	    getLog().error("setup classpath: ", ex);
	    return "";
	}

	cp.add(project.getBuild().getOutputDirectory());

	cp.add(findToolsJar().getAbsolutePath());

	Iterator<Comparable<String>> cpIter = cp.iterator();
	StringBuffer oBuilder = new StringBuffer(2000);
	oBuilder.append(String.valueOf(cpIter.next()));
	while (cpIter.hasNext())
	    oBuilder.append(File.pathSeparator).append(cpIter.next());

	return oBuilder.toString();
    }

    /**
     * Figure out where the tools.jar file lives.
     */
    private File findToolsJar() {

	File file = null;
	if (tcManager != null) {
	    Toolchain toolchain = tcManager.getToolchainFromBuildContext("jdk", session);
	    if (toolchain != null) {
		String pathToJavaC = toolchain.findTool("javac");
		File f = new File(pathToJavaC);

		String relativePath = "lib/tools.jar";
		if (SystemUtils.IS_OS_MAC_OSX) {
		    relativePath = "Classes/classes.jar";
		}

		File toolsJarFile = new File(f.getParentFile().getParentFile(), relativePath);
		if (toolsJarFile.exists() && toolsJarFile.isFile()) {
		    file = toolsJarFile;
		}
	    }
	}

	File javaHome = new File(System.getProperty("java.home"));

	if (file == null) {
	    try {
		if (SystemUtils.IS_OS_MAC_OSX) {
		    file = new File(javaHome, "../Classes/classes.jar").getCanonicalFile();
		} else {
		    file = new File(javaHome, "../lib/tools.jar").getCanonicalFile();
		}
	    } catch (IOException ex) {
		getLog().error("Couldn't find tools.jar.", ex);
	    }
	}

	if ((file == null) || !file.exists()) {
	    getLog().error("Missing tools.jar at: $file");
	}

	getLog().debug("Using tools.jar: " + file);

	return file;
    }

}
