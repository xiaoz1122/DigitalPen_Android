package com.smart.pen.core.symbol;
/**
 * 
 * @author Xiaoz
 * @date 2015年10月22日 上午8:38:59
 *
 * Description
 */
public enum RecordLevel {
	MEDIUM(0),
	HIGH(1),
	LOW(2);
	
	private final int value;

    private RecordLevel(int value) {
        this.value = value;
    }

    public final int getValue() {
        return value;
    }
    
    /**
     * int转换为RecordLevel，如果溢出那么输出MEDIUM
     * @param value 需要转换的int值
     * @return
     */
    public static RecordLevel toRecordLevel(int value){
    	if(value >= 0 && value < RecordLevel.values().length){
    		return RecordLevel.values()[value];
    	}else{
    		return MEDIUM;
    	}
    }
}
