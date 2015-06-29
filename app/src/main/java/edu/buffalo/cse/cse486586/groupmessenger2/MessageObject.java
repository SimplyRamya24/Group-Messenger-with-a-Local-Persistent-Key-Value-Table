package edu.buffalo.cse.cse486586.groupmessenger2;
import java.util.Comparator;
/**
 * Created by ramya on 3/11/15.
 */
public class MessageObject {

    public String mType = null;
    public String mMsg = null;
    public int mPriority;
    public int mProcNum;
    public int mComingFromProcess;
    public int receipt;
    public int mMsgId;
    public String deliverable;

    public MessageObject() {
        this.receipt = 0;
        this.deliverable = "N";
    }

    public static Comparator<MessageObject> priorityComparator = new Comparator<MessageObject>() {
        public int compare(MessageObject m1,MessageObject m2) {
            int priority1 = m1.mPriority;
            int processNum1 = m1.mProcNum;
            int processNum2 = m2.mProcNum;
            int msgID1 = m1.mMsgId;
            int msgID2 = m2.mMsgId;
            int priority2 = m2.mPriority;
            if (priority1 == priority2) {
                return processNum1-processNum2;
            }else {
                return priority1-priority2;
            }
        }
    };

    @Override
    public String toString() {
        return mType + "::"+
               mPriority +"::"+
               mProcNum +"::"+
               mMsgId +"::"+
               mComingFromProcess+"::"+
               deliverable +"::"+
               mMsg;
    }
}
