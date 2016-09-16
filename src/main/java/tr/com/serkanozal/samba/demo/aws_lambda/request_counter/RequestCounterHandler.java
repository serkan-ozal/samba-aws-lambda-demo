package tr.com.serkanozal.samba.demo.aws_lambda.request_counter;

import tr.com.serkanozal.samba.SambaField;
import tr.com.serkanozal.samba.SambaFieldProcessor;
import tr.com.serkanozal.samba.cache.SambaCacheType;

import com.amazonaws.services.lambda.runtime.Context;

/**
 * This handler is an AWS Lamda function that shares and processes 
 * a request counter atomically and globally through <b>Samba</b>.
 * 
 * In this handler, a long typed field (request counter) is shared globally via 
 * <b>TIERED</b> cache backed <b>Samba</b> field.
 * Each lambda function invocation can increase this shared field value atomically and
 * can retrieve its value with <b>eventual consistency</b> guarantee 
 * (means that every field access might not return fresh value 
 * but it will returns fresh value eventually).
 */
public class RequestCounterHandler {

    private static final int GET_REQUEST = 1;
    private static final int POST_REQUEST = 2;
    
    // ********** NOTE **********
    // Id of this field is auto generated based on the source code location 
    // (<class-name, method-name, line-number>) of this field definition.
    // If the line number changes, anymore this field represents different field.
    // For handling such cases, you may give an explicit id to the field as 
    // first constructor argument.
    private final SambaField<Long> requestCounterField = 
            new SambaField<Long>(SambaCacheType.TIERED);
    private final SambaFieldProcessor<Long> requestCounterProcessor =
            new RequestCounterProcessor();
    
    public long handle(int requestType, Context context) {
        switch (requestType) {
            case GET_REQUEST:
                // Since cache type is `TIERED`, 
                // consistency model of the counter field is "eventual-consistency".
                // Therefore, "get" call might return old value but it will return latest value eventually.
                // Due to our demo logic, we assume that 
                // sometimes returning old request count value is not problem. 
                // If we want fresh value of it every time, we can use "refresh" call instead of "get" call.
                return returnCounterValue(requestCounterField.get());
            case POST_REQUEST:
                // Due to our demo logic, we keep counter only for "POST" requests.
                // After each increment, we return fresh value of it.
                // Our assumption is that "POST" requests are not too much as "GET" requests.
                // So we support "strong-consistency" model for return value on "POST" requests 
                // by accepting more latency overhead than "GET" requests,
                // but we support "eventual-consistency" model for return value on "GET" requests
                // since we expect much too much "GET" requests due to our demo logic.
                requestCounterField.processAtomically(requestCounterProcessor);
                return returnCounterValue(requestCounterField.refresh());
            default:
                throw new IllegalArgumentException("Invalid request type. " + 
                                                   "Can only be '1' ('GET') or '2' (POST)'!");
        }
    }
    
    private long returnCounterValue(Long value) {
        if (value == null) {
            return 0L;
        } else {
            return value;
        }
    }
    
    private static class RequestCounterProcessor implements SambaFieldProcessor<Long> {

        @Override
        public Long process(Long currentValue) {
            if (currentValue == null) {
                return 1L;
            } else {
                return currentValue + 1;
            }
        }
        
    }

}
