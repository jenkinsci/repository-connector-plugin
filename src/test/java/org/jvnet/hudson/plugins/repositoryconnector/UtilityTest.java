package org.jvnet.hudson.plugins.repositoryconnector;

import java.util.HashMap;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import hudson.util.VariableResolver;

public class UtilityTest {

    private VariableResolver.ByMap<String> resolver;

    @Before
    public void setUp() throws Exception {
        HashMap<String, String> valueMap = new HashMap<String, String>();
        valueMap.put("FOO", "42");
        resolver = new VariableResolver.ByMap<String>(valueMap);
    }

    @Test
    public void resolveVariableShouldWorkForNix() throws Exception {
        String value = Utility.resolveVariable(resolver, "${FOO}");
        Assert.assertEquals("42", value);
    }

    @Test
    public void resolveVariableShouldLeaveUnchangedForNixMatchAtStartOfString() throws Exception {
        String potentialVariable = "${FOO}x";
        Assert.assertEquals(potentialVariable, Utility.resolveVariable(resolver, potentialVariable));
    }

    @Test
    public void resolveVariableShouldLeaveUnchangedForNixMatchInMiddleOfString() throws Exception {
        String potentialVariable = "x${FOO}x";
        Assert.assertEquals(potentialVariable, Utility.resolveVariable(resolver, potentialVariable));
    }

    @Test
    public void resolveVariableShouldLeaveUnchangedForNixMatchAtEndOfString() throws Exception {
        String potentialVariable = "x${FOO}";
        Assert.assertEquals(potentialVariable, Utility.resolveVariable(resolver, potentialVariable));
    }

    @Test
    public void resolveVariableShouldWorkForWin() throws Exception {
        String value = Utility.resolveVariable(resolver, "%FOO%");
        Assert.assertEquals("42", value);
    }

    @Test
    public void resolveVariableShouldLeaveUnchangedForWinMatchAtStartOfString() throws Exception {
        String potentialVariable = "%FOO%x";
        Assert.assertEquals(potentialVariable, Utility.resolveVariable(resolver, potentialVariable));
    }

    @Test
    public void resolveVariableShouldLeaveUnchangedForWinMatchInMiddleOfString() throws Exception {
        String potentialVariable = "x%FOO%x";
        Assert.assertEquals(potentialVariable, Utility.resolveVariable(resolver, potentialVariable));
    }

    @Test
    public void resolveVariableShouldLeaveUnchangedForWinMatchAtEndOfString() throws Exception {
        String potentialVariable = "x%FOO%";
        Assert.assertEquals(potentialVariable, Utility.resolveVariable(resolver, potentialVariable));
    }
}
