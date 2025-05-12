package com.htc.licenseapproval.dto;

import lombok.Data;

@Data
public class BUResponseDTO<T> {
	private int count;
	private float totalHoursSpent;
	private T data;
	

}
