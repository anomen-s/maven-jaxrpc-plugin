package net.sf.jaxrpcmaven.jaxrpc;

import com.sun.xml.rpc.tools.wscompile.CompileTool;

import java.util.*;
import java.io.File;
import org.apache.maven.artifact.DependencyResolutionRequiredException;

import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugin.logging.Log;
import org.apache.maven.project.MavenProject;

/**
 * A wscompile mojo for maven 2.
 *
 * @author <a href="ludek.h@gmail.com">Ludek Hlavacek</a>
 * @goal wscompile
 * @requiresDependencyResolution runtime
 * @requiresProject
 * @phase process-classes
 * @describe wscompile mojo
 */
public class WscompileMojo extends AbstractMojo {

	/**
	 * The maven project.
	 * 
	 * @parameter expression="${project}"
	 * @required
	 * @readonly
	 */
	private MavenProject project;

	/**
	 * operation
	 * 
	 * @parameter
	 * @required
	 */
	private String operation;

	/**
	 * enable the given features
	 *
	 * @parameter
	 */
	private String features;

	/**
	 * specify a HTTP proxy server
	 *
	 * @parameter
	 */
	private ProxyConfiguration httpProxy;

    /**
     * output messages about what the compiler is doing
     *
     * @parameter default-value="false"
     */
    private boolean verbose;

    /**
     * keep generated files
     *
     * @parameter default-value="false"
     */
    private boolean keep;

    /**
     * generate debugging info
     *
     * @parameter default-value="false"
     */
    private boolean debug;

    /**
     * optimize generated code
     *
     * @parameter default-value="false"
     */
    private boolean optimize;

	/**
	 * generate a J2EE mapping.xml file
	 * 
	 * @parameter
	 */
	private File mapping;


	/**
	 * write the internal model to the given file
	 * 
	 * @parameter
	 */
	private File model;

	/**
	 * specify where to place non-class generated files
	 * 
	 * @parameter
	 */
	private File nd;

	/**
	 * specify where to place generated source files
	 *
	 * @parameter default-value="${project.build.directory}/generated-sources/jaxrpc"
	 */
	private File s;

	/**
	 * specify where to place generated output files
	 *
	 * @parameter default-value="${project.build.outputDirectory}"
	 */
	private File d;

	/**
	 * Generate code for the specified JAX-RPC SI version. 
	 * Supported versions are: 1.0.1, 1.0.3, and 1.1 (default).
	 *
	 * @parameter
	 */
	private String source;


	/**
	 * configuration-file
	 *
	 * @required
	 * @parameter
	 */
	private String config;


	public void execute() throws MojoExecutionException {
		Log log = getLog();
		
		List args = new ArrayList();

		args.add("-"+operation);
		
		args.add("-cp");
		args.add(getCp() /*project.getProperties().getProperty("project.build.outputDirectory")*/);

		if (features != null) {
		    args.add("-features:"+features);
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
		    args.add("-httpproxy:"+httpProxy.getHost()+":"+httpProxy.getPort());
		}
		
		if (mapping != null) {
		    args.add("-mapping");
		    args.add(mapping.toString());
		}
		if (model != null) {
		    args.add("-model");
		    args.add(model.toString());
		}
		if (nd != null) {
		    args.add("-nd");
		    args.add(nd.toString());
		    nd.mkdirs();
		}
		if (s != null) {
		    args.add("-s");
		    args.add(s.toString());
		    s.mkdirs();
		}
		if (d != null) {
		    args.add("-d");
		    args.add(d.toString());
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
		
		String[] strArgs = (String[]) args.toArray(new String[args.size()]);
		getLog().info(Arrays.toString(strArgs));
		
		CompileTool tool = new CompileTool(System.out, "wscompile");
		getLog().info("wscompile " + ":" + tool.run(strArgs));
	}

	
	void setProject(MavenProject project) {
		this.project = project;
	}

        
    /**
    * Returns the an isolated classloader.
    *
    * @return ClassLoader
    * @noinspection unchecked
    */
    protected String getCp()
    {

        Set cp = new HashSet();

        try {
	    List classpathElements = project.getCompileClasspathElements();
            for (int i = 0; i < classpathElements.size(); i++)
            {
                cp.add(classpathElements.get(i));
            }
        } catch (DependencyResolutionRequiredException ex) {
            getLog().error("setup classpath: ", ex);
            return "";
        }

        cp.add(project.getBuild().getOutputDirectory());
    
        Iterator cpIter = cp.iterator();
        StringBuffer oBuilder = new StringBuffer(2000);
        oBuilder.append(String.valueOf(cpIter.next()));
        while ( cpIter.hasNext() )
            oBuilder.append( File.pathSeparator ).append( cpIter.next() );
     
        return oBuilder.toString();
    }

}
