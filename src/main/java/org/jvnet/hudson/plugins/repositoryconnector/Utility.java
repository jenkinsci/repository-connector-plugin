package org.jvnet.hudson.plugins.repositoryconnector;

import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import hudson.util.VariableResolver;

public class Utility {

	static Logger log = Logger.getLogger(Utility.class.getName());
    private static final Pattern UNIX_VARIABLE_PATTERN = Pattern.compile("^\\$\\{(.*)\\}$");
    private static final Pattern WINDOHS_VARIABLE_PATTERN = Pattern.compile("^%(.*)%$");

	public static String resolveVariable(VariableResolver<String> variableResolver, String potentialVariable) {
		String value = potentialVariable;
		if (potentialVariable != null) {
            value = resolveForPattern(variableResolver, potentialVariable, UNIX_VARIABLE_PATTERN);
            if(potentialVariable.equals(value)) {
                value = resolveForPattern(variableResolver, potentialVariable, WINDOHS_VARIABLE_PATTERN);
            }
        }
		return value;
	}

    private static String resolveForPattern(VariableResolver<String> variableResolver, String potentialVariable, Pattern pattern) {
        Matcher matcher = pattern.matcher(potentialVariable);
        if(!matcher.matches()) {
            return potentialVariable;
        }
        String value = matcher.group(matcher.groupCount());
        value = variableResolver.resolve(value);
        log.log(Level.FINE, "resolve " + potentialVariable + " to " + value);
        return value;
    }

}
