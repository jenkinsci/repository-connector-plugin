package org.jvnet.hudson.plugins.repositoryconnector.util;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.Arrays;

import org.junit.Test;
import org.jvnet.hudson.plugins.repositoryconnector.Repository;

import hudson.util.ListBoxModel.Option;

public class RepositoryListBoxTest {

    @Test
    public void testListBox() {
        RepositoryListBox listbox = new RepositoryListBox(Arrays.asList(Repository.MAVEN_CENTRAL));

        assertEquals(1, listbox.size());
        verifyCentral(listbox.get(0));
    }

    @Test
    public void testListBoxWithSelectAll() {
        RepositoryListBox listbox = new RepositoryListBox(Arrays.asList(Repository.MAVEN_CENTRAL))
                .withSelectAll();

        assertEquals(2, listbox.size());
        
        verifySelectAll(listbox.get(0));
        verifyCentral(listbox.get(1));
    }
    
    private void verifyCentral(Option option) {
        assertEquals("central", option.value);
        assertTrue(option.name.contains("central - http"));
    }
    
    private void verifySelectAll(Option option) {
        assertEquals("", option.value);
        assertTrue(option.name.contains("all repositories"));
    }
}
