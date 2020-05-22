package com.dalerrowney.mailchimp;

import java.io.InputStream;
import java.io.IOException;
import java.security.*;
import java.util.*;
import java.nio.charset.StandardCharsets;
import java.math.BigInteger;
import okhttp3.*;
import com.squareup.moshi.Moshi;
import com.dalerrowney.Logger;
import com.squareup.moshi.JsonAdapter;

public class MailChimpAPIClient {
    public static Moshi Moshi = new Moshi.Builder().build();
    public static JsonAdapter<MemberStatus> MemberStatusAdapter = null;

    protected boolean _isConfigured = false;
    protected String _apiEndpoint;
    protected String _apiUser;
    protected String _apiKey;
    protected String _defaultListId;
    protected String _defaultSubscriberStatus;

    protected Moshi _moshi;
    protected OkHttpClient _client;
    protected Credentials  _auth;
	private HashMap<String, String> _interestsMap;

    public MailChimpAPIClient() {
        try {
            ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
            InputStream in = classLoader.getResourceAsStream("mailchimp_api.properties");
            Properties config = new Properties();
            config.load(in);
            init(config);
        } catch(IOException e) {
            // todo
        }            
    }
    public MailChimpAPIClient(Properties config) {
        init(config);
    }
    private void init(Properties config) {
        _apiEndpoint = config.getProperty("api_endpoint");
        _apiUser = config.getProperty("api_user");
        _apiKey = config.getProperty("api_key");
        _defaultListId = config.getProperty("list_id");
        _defaultSubscriberStatus = config.getProperty("default_subscriber_status", "pending");
        
        _interestsMap = new HashMap<String, String>();
        Enumeration propNames = config.propertyNames();
        while(propNames.hasMoreElements()) {
        	String prop = (String) propNames.nextElement();
        	if(prop.startsWith("inspire")) {
        		String mapping = config.getProperty(prop);
        		if(mapping != null && mapping.length() > 0) 
        			_interestsMap.put(prop, config.getProperty(prop));
        	}
        }

        this._client =
            new OkHttpClient.Builder()
            .authenticator(new Authenticator() {
                public Request authenticate(Route route, Response response) throws IOException {
                    if(response.request().header("Authorization") != null) {
                        return null; // Give up, have already attempted to authenticate.
                    }
                    return response.request().newBuilder()
                    .header("Authorization", Credentials.basic(_apiUser, _apiKey))
                    .build();
                }
            })
            .build();    
        _isConfigured = true;        
    }

    private JsonAdapter<MemberStatus> getMemberStatusAdapter() {
        if(MemberStatusAdapter == null) {
            MemberStatusAdapter = Moshi.adapter(MemberStatus.class);
        }
        return MemberStatusAdapter;
    }

    public String generateMemberId(String emailAddress) {
        String result = null;
        if(emailAddress != null && emailAddress.length() > 0) {
            try {
                String input = emailAddress.toLowerCase();
                MessageDigest digest = MessageDigest.getInstance("MD5");
                byte[] hash = digest.digest(input.getBytes(StandardCharsets.UTF_8));
                result = String.format("%032x", new BigInteger(1, hash));
            }
            catch(NoSuchAlgorithmException e) {
                // todo
            }
        }
        return result;
    }   

    public MemberStatus checkMemberStatus(String emailAddress, Logger logger) {
        MemberStatus result = new MemberStatus(emailAddress, "unknown", (List<String>)null);
        String memberId = generateMemberId(emailAddress);
        logger.log("checkMemberStatus: email="+emailAddress+"; memberId="+memberId);
        if(memberId != null) {            
            Request request =
                new Request.Builder()
                .url("https://" + this._apiEndpoint + "/3.0/lists/" + this._defaultListId + "/members/"+memberId+"?fields=status,interests,email_address")
                .build();
            try {
                Response resp = this._client.newCall(request).execute();
                logger.log("checkMemberStatus: status="+resp.code());                
                if (resp.isSuccessful()) {
                    result = getMemberStatusAdapter().fromJson(resp.body().string());
                }
                else {
                	logger.log("checkMemberStatus: message="+resp.message());
                }
            }
            catch(IOException e) {
            	logger.log("checkMemberStatus: exception=" + e.getMessage());
            }
        } else {
        	logger.log("checkMemberStatus: could not generate a member id");
        }
        return result;
    }
    
    private List<String> mapInterests(List<String> interests) {
    	List<String> result = new LinkedList<String>();
    	for(String interest : interests) {
    		if(_interestsMap.containsKey(interest))
    			result.add(_interestsMap.get(interest));
    	}    	
    	return result;
    }
    
    public int registerMember(String emailAddress, String fname, String lname, List<String> interests, Logger logger) {
    	int result = 400;
        MemberStatus currentStatus = checkMemberStatus(emailAddress, logger);
  
        if("unknown".equals(currentStatus.status)) {
            result = createMember(emailAddress, fname, lname, interests, logger);
        } else {
            result = updateMember(emailAddress, fname, lname, interests, currentStatus, logger);
        }
        return result;    	
    }
    
    private int createMember(String emailAddress, String fname, String lname, List<String> interests, Logger logger) {   
        int result = 400;
        MemberStatus newMember = new MemberStatus(emailAddress, _defaultSubscriberStatus, mapInterests(interests));
        newMember.merge_fields.put("FNAME", fname);
        newMember.merge_fields.put("LNAME", lname);
        String body = getMemberStatusAdapter().toJson(newMember);
        String memberId = generateMemberId(emailAddress);
        logger.log("createMember: email="+emailAddress+"; memberId="+memberId+"; interests="+interests);
        if(memberId != null) {
        	logger.log("createMember: body="+body);        	
            Request request =
                new Request.Builder()
                .url("https://" + this._apiEndpoint + "/3.0/lists/" + this._defaultListId + "/members")
                .method("POST", RequestBody.create(body, MediaType.get("application/json")))
                .build();
            try {
                Response resp = this._client.newCall(request).execute();
                logger.log("createMember: status="+resp.code());                
                result = resp.code();
                if(!resp.isSuccessful()) {
                	logger.log("createMember: message="+resp.message());                	
                }
            }
            catch(IOException e) {
            	logger.log("createMember: exception="+e.getMessage());
            }
        }
        return result;        
    }

    private int updateMember(String emailAddress, String fname, String lname, List<String> interests, MemberStatus currentStatus, Logger logger) {    	
        int result = 400;
        
        Map<String, Boolean> newInterests = new HashMap<String, Boolean>();

        // set all current interest to false
        for(String interest : currentStatus.interests.keySet()) {
        	newInterests.put(interest, Boolean.FALSE);
        }
        // add new interests
        for(String interest: mapInterests(interests)) {
        	newInterests.put(interest, Boolean.TRUE);
        }        
        
        MemberStatus newMember = new MemberStatus(null, null, newInterests);
        String body = getMemberStatusAdapter().toJson(newMember);

        String memberId = generateMemberId(emailAddress);
        
        logger.log("updateMember: email="+emailAddress+"; memberId="+memberId+"; interests="+interests);
        if(memberId != null) {        
        	logger.log("updateMember: body="+body);   
            Request request =
                new Request.Builder()
                .url("https://" + this._apiEndpoint + "/3.0/lists/" + this._defaultListId + "/members/"+memberId)
                .method("PUT", RequestBody.create(body, MediaType.get("application/json")))
                .build();
            try {
                Response resp = this._client.newCall(request).execute();
                logger.log("updateMember: status="+resp.code());
                result = resp.code();
                if(!resp.isSuccessful()) {
                	logger.log("updateMember: message="+resp.message());                	
                }
            }
            catch(IOException e) {
            	logger.log("updateMember: exception="+e.getMessage());
            }
        }
        return result;  
    }     
    
    /**
     * NOTE: MailChimp will error if you try to delete a contact who has a status of "pending" 
     */
    public int deleteMember(String emailAddress, Logger logger) {
        int result = 400;
        logger.log("deleteMember: email="+emailAddress); 
        MemberStatus member = this.checkMemberStatus(emailAddress, logger);
        if(member != null && !"pending".equals(member.status))
        {
        	String memberId = generateMemberId(emailAddress);
            Request request =
                new Request.Builder()
                .url("https://" + this._apiEndpoint + "/3.0/lists/" + this._defaultListId + "/members/"+memberId)
                .method("DELETE", null)
                .build();
            try {
                Response resp = this._client.newCall(request).execute();
                result = resp.code();
                if(!resp.isSuccessful()) {
                	logger.log("deleteMember: message="+resp.message());                	
                }
            }
            catch(IOException e) {
            	logger.log("deleteMember: exception="+e.getMessage());
            }
        }
        else {
        	logger.log("deleteMember: cannot delete members where status=pending");        	
        }
        return result;          
    }
}