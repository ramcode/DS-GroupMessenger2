package edu.buffalo.cse.cse486586.groupmessenger2;

import java.io.Serializable;
import java.util.Comparator;

/**
 * Created by ramesh on 3/17/16.
 */
public class MessageComparator implements Comparator<Message>, Serializable {
@Override
    public int compare(Message m1, Message m2){
        Double m1_seq = m1.getSequencer();
        Double m2_seq = m2.getSequencer();
    if(m1_seq<m2_seq){
        return  -1;
    }
    else if(m1_seq>m2_seq){
        return 1;
    }
    else {
        return 0;
    }

}

}
