package com.dalerrowney.mailchimp;

import java.io.InputStream;
import java.util.Arrays;
import junit.framework.TestCase;
import java.util.Properties;
import com.dalerrowney.Logger;

/**
 *
 * @author marcusedwards
 */
public class MailChimpAPIClientTest extends TestCase {
    protected MailChimpAPIClient client;
    
    protected String testEmailAddress = "test.account@acme.com";
    protected String testFName        = "test";
    protected String testLName        = "account";
    protected String testMemberId     = "1fb2fc8366c9e4d3521f147652cc3980";
    protected String testStatus       = "unsubscribed";
    protected Logger logger           = new Logger();
    
    public MailChimpAPIClientTest(String testName) {
        super(testName);
    }
    
    @Override
    protected void setUp() throws Exception {
        super.setUp();
        ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
        InputStream in = classLoader.getResourceAsStream("mailchimp_api.properties");
        Properties config = new Properties();
        config.load(in);
        
        // override for the test otherwise we aren't able to clean up the test created subscribers
        config.replace("default_subscriber_status", testStatus);
        
        client = new MailChimpAPIClient(config);        
    }
    
    @Override
    protected void tearDown() throws Exception {
        super.tearDown();
    }

    public void testGenerateMemberId() {
        System.out.println("generateMemberId");

        String result = client.generateMemberId(testEmailAddress);
        assertEquals(testMemberId, result);
    }
    
    public void testGenerateMemberIdMixedCase() {
        System.out.println("generateMemberIdMixedCase");
        String emailAddress = "Urist.McVankab@freddiesjokes.com";
        String memberId = "62eeb292278cc15f5817cb78f7790b08";

        String result = client.generateMemberId(emailAddress);
        assertEquals(memberId, result);
    }   
    
    public void testGenerateMemberIdNull() {
        System.out.println("generateMemberIdMixedCase");
        String emailAddress = null;
        String memberId = null;

        String result = client.generateMemberId(emailAddress);
        assertEquals(memberId, result);
    }     
    
    public void testGenerateMemberIdEmpty() {
        System.out.println("generateMemberIdMixedCase");
        String emailAddress = "";
        String memberId = null;

        String result = client.generateMemberId(emailAddress);
        assertEquals(memberId, result);
    }     

    public void testCheckMemberStatus() {
        System.out.println("checkMemberStatus");
        String emailAddress = "marcus.edwards@crownpeak.com";
        String expResult = "pending";
        MemberStatus result = client.checkMemberStatus(emailAddress, logger);
        System.out.println(logger.toString());
        assertNotNull(result);
        assertEquals(expResult, result.status);
        assertEquals(emailAddress, result.emailAddress);
        assertEquals(10, result.interests.size());
        
    }

    public void testCheckMemberStatusUnknown() {
        System.out.println("checkMemberStatus");
        String emailAddress = "unknown@acme.com";
        String expResult = "unknown";
        MemberStatus result = client.checkMemberStatus(emailAddress, logger);
        System.out.println(logger.toString());
        assertNotNull(result);
        assertEquals(expResult, result.status);
    }    
    
    public void testCheckMemberStatusNull() {
        System.out.println("checkMemberStatus");
        String emailAddress = null;
        String expResult = "unknown";
        MemberStatus result = client.checkMemberStatus(emailAddress, logger);
        System.out.println(logger.toString());
        assertNotNull(result);
        assertEquals(expResult, result.status);
    }   

    public void testCheckMemberStatusEmpty() {
        System.out.println("checkMemberStatus");
        String emailAddress = "";
        String expResult = "unknown";
        MemberStatus result = client.checkMemberStatus(emailAddress, logger);
        System.out.println(logger.toString());
        assertNotNull(result);
        assertEquals(expResult, result.status);
    }   
    
    public void testRegisterNewMember() {
    	String emailAddress = "test." + System.currentTimeMillis() + "@acme.com";
        System.out.println("testRegisterNewMember: " + emailAddress);
        
        int result = client.registerMember(emailAddress, testFName, testLName, Arrays.asList("inspire-un-professional", "inspire-ut-acrylic", "inspire-us-paper"), logger);
        assertTrue(result >= 200 && result < 210);
        
        MemberStatus member = client.checkMemberStatus(emailAddress, logger);
        assertNotNull(member);
        assertEquals(testStatus, member.status);
        long assignedInterests = member.interests.values().stream().filter(v -> v.equals(Boolean.TRUE)).count();     
        assertEquals(3, assignedInterests);
        
        // clean up the test member -- can only do an 'archive' as perma-delete means you cannot resub the email address        
        int cleanupResult = client.deleteMember(emailAddress, logger);
        System.out.println(logger.toString());
        
        assertTrue(cleanupResult >= 200 && cleanupResult < 210);
    }

    public void testRegisterExistingMember() {
        System.out.println("testRegisterExistingMember");
        String newInterest = "inspire-ut-ink";
        String newMailChimpInterest = "9489c6aa2a";
        
        MemberStatus member = client.checkMemberStatus(testEmailAddress, logger);
        assertNotNull(member);
        
        int result = client.registerMember(testEmailAddress, testFName, testLName, Arrays.asList(newInterest), logger);
        assertEquals(200, result);
        
        MemberStatus updatedMember = client.checkMemberStatus(testEmailAddress, logger);
        assertNotNull(updatedMember);
        assertEquals(testStatus, updatedMember.status);
        long assignedInterests = member.interests.values().stream().filter(v -> v.equals(Boolean.TRUE)).count();     
        assertEquals(1, assignedInterests);        
        assertTrue(updatedMember.interests.get(newMailChimpInterest));
        System.out.println(logger.toString());

        // clean up the test member -- can only do an 'archive' as perma-delete means you cannot resub the email address        
    }
    
}
