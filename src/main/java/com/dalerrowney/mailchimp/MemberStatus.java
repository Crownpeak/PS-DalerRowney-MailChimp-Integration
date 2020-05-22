package com.dalerrowney.mailchimp;

import com.squareup.moshi.Json;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class MemberStatus {
    @Json(name="email_address")
    public String emailAddress;
    public String status;
    public Map<String, Boolean> interests;
    public Map<String, String> merge_fields = new LinkedHashMap<String, String>();
    
    public MemberStatus() {
        
    }
    
    public MemberStatus(String emailAddress, String status, List<String> interests) {
        Map<String, Boolean> interestsMap = new LinkedHashMap<String, Boolean>();
        if(interests != null) {
            for (String interest : interests) {
                interestsMap.put(interest, Boolean.TRUE);
            }
        }
        init(emailAddress, status, interestsMap);
    }
    
    public MemberStatus(String emailAddress, String status, Map<String, Boolean> interests) {
        init(emailAddress, status, interests==null ? Collections.EMPTY_MAP : interests);
    }
    
    private void init(String emailAddress, String status, Map<String, Boolean> interests) {
        this.emailAddress = emailAddress;        
        this.status = status;
        this.interests = interests;
    }
}