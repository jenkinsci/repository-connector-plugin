package org.jvnet.hudson.plugins.repositoryconnector;

import hudson.util.VariableResolver;

import java.util.logging.Level;
import java.util.logging.Logger;

public class Utility {

	static Logger log = Logger.getLogger(Utility.class.getName());

	public static String resolveVariable(VariableResolver<String> variableResolver, String potentalVaraible) {
		String value = potentalVaraible;
		if (potentalVaraible != null) {
			if (potentalVaraible.startsWith("${") && potentalVaraible.endsWith("}")) {
				value = potentalVaraible.substring(2, potentalVaraible.length() - 1);
				value = variableResolver.resolve(value);
				log.log(Level.FINE, "resolve " + potentalVaraible + " to " + value);
			}
		}
		return value;
	}

}
