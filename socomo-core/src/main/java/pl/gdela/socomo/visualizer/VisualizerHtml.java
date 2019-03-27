package pl.gdela.socomo.visualizer;

import java.util.ArrayList;
import java.util.List;

import pl.gdela.socomo.composition.Component;
import pl.gdela.socomo.composition.ComponentDep;
import pl.gdela.socomo.composition.Level;
import pl.gdela.socomo.composition.Module;

import static java.lang.String.format;
import static java.util.Locale.ENGLISH;
import static org.apache.commons.lang3.StringUtils.repeat;
import static org.apache.commons.lang3.StringUtils.split;
import static org.apache.commons.text.StringEscapeUtils.escapeEcmaScript;
import static org.apache.commons.text.StringEscapeUtils.escapeHtml4;

/**
 * Template for generating html file which "launches" visualizer single-page app.
 *
 * <p>We do not use freemarker, mustache or any other template, because we need complete
 * control over every space, newline, etc., as the generated html file is supposed to be
 * committed to the source repository and it also needs to be human-readable, or even
 * beautiful.</p>
 */
class VisualizerHtml {
	private final Module module;
	private final List<Level> levels = new ArrayList<>();
	private final List<Asset> assets = new ArrayList<>();

	private StringBuilder out;
	private String indent = "";

	VisualizerHtml(Module module) {
		this.module = module;
	}

	void addLevel(Level level) {
		levels.add(level);
	}

	void addAsset(Asset asset) {
		assets.add(asset);
	}

	String render() {
		out = new StringBuilder();
		printHtml();
		return out.toString();
	}

	private void printHtml() {
		out("<!-- File autogenerated by SoCoMo (https://github.com/gdela/socomo): do not edit by hand, but do commit to repository -->");
		out("<!doctype html>");
		out("<!--suppress ALL-->");
		out("<html lang='en'>");
		printHead();
		printBody();
		out("</html>");
	}

	private void printHead() {
		out("<head>");
		indent(+4);
		out("<title>SoCoMo: %s</title>", escapeHtml(module.name));
		for (Asset asset : assets) {
			switch (asset.type) {
				case STYLE:
					out("<link href='%s' rel='stylesheet'>", asset.url);
					break;
				case SCRIPT:
					out("<script src='%s'></script>", asset.url);
					break;
				case STYLE_CONTENT:
					out("<style>");
					for (String line : split(asset.content, '\n')) out(line);
					out("</style>");
					break;
				case SCRIPT_CONTENT:
					out("<script>");
					for (String line : split(asset.content, '\n')) out(line);
					out("</script>");
					break;
				default:
					throw new IllegalArgumentException("unsupported asset type " + asset.type);
			}
		}
		indent(-4);
		out("</head>");
	}

	private void printBody() {
		out("<body>");
		out("<script>");
		out("socomo(%s, { // module", ecmaString(module.name));
		for (Level level : levels) {
			out("");
			printLevel(level);
		}
		out("");
		out("});");
		out("</script>");
		out("</body>");
	}

	private void printLevel(Level level) {
		out("[%s]: // level", ecmaString(level.name));
		out("{");
		indent(+2);
		printComponents(level);
		printDependencies(level);
		indent(-2);
		out("},");
	}

	private void printComponents(Level level) {
		float maxSize = level.maxComponentSize();
		out("components: {");
		indent(+2);
		for (Component component : level.components) {
			out("%-36s :{ size: %.1f },", ecmaString(component.name), component.size/maxSize);
		}
		indent(-2);
		out("},");
	}

	private void printDependencies(Level level) {
		float maxStrength = level.maxDependencyStrength();
		out("dependencies: {");
		indent(+2);
		for (ComponentDep dep : level.dependencies) {
			String depName = dep.from.name + " -> " + dep.to.name;
			out("%-36s :{ strength: %.1f },", ecmaString(depName), dep.strength/maxStrength);
		}
		indent(-2);
		out("},");
	}

	private void indent(int change) {
		int newSize = indent.length() + change;
		indent = repeat(' ', newSize);
	}

	private void out(String line, Object... args) {
		out.append(indent).append(format(ENGLISH, line, args)).append("\n");
	}

	private static String ecmaString(String contents) {
		return "'" + escapeEcmaScript(contents) + "'";
	}

	private static String escapeHtml(String contents) {
		return escapeHtml4(contents);
	}
}
