package pl.gdela.socomo.maven;

import java.io.File;

import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.Component;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.project.MavenProject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import pl.gdela.socomo.SocomoFacade;

/**
 * Common things for Socomo maven plugins.
 */
abstract class SocomoMojo extends AbstractMojo {

	private static final Logger log = LoggerFactory.getLogger(SocomoMojo.class);

	/**
	 * Whether to skip socomo step altogether. Useful as a temporary way to build your
	 * project even in case of modularity errors, for example during refactoring.
	 */
	@Parameter(property = "socomo.skip", defaultValue = "false")
	private boolean skip;

	/**
     * Directory containing *.class files with java bytecode to visualize.
     */
	@Parameter(defaultValue = "${project.build.outputDirectory}", readonly = true)
	private File bytecodeDirectory;

	@Component
	private MavenProject mavenProject;

	/**
	 * Location of the {@code socomo.html} file to which results will be written.
	 */
	File socomoTargetFile;

	@Override
	public final void execute() throws MojoExecutionException {
		if (mavenProject.getPackaging().equals("pom")) { return; }
		if (skip) { log.warn("skipping socomo"); return; }

    	if (!bytecodeDirectory.exists()) {
			log.warn("bytecode directory {}", bytecodeDirectory);
			log.warn("skipping socomo in this module, the bytecode directory is missing");
			return;
		}
		socomoTargetFile = new File(mavenProject.getBasedir(), "socomo.html");

		try {
			beforeExecute();
			SocomoFacade socomo = new SocomoFacade(mavenProject.getName());
			socomo.analyzeBytecode(bytecodeDirectory);
			socomo.guessLevel();
			socomo.visualizeInto(socomoTargetFile);
			afterExecute();
		} catch (RuntimeException e) {
			throw new MojoExecutionException(e.getMessage(), e);
		}
    }

	abstract void beforeExecute();

	abstract void afterExecute();

}
