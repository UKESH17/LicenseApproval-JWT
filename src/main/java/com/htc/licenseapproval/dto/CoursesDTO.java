package com.htc.licenseapproval.dto;

import java.util.Set;

import lombok.Data;
import lombok.RequiredArgsConstructor;

@Data
@RequiredArgsConstructor
public class CoursesDTO {
	
	public CoursesDTO(float hoursSpent) {
		// TODO Auto-generated constructor stub
		this.hoursSpent = hoursSpent;
	}

	private boolean certificateDone;

	private String courseDetails;

	private float hoursSpent;
	
	private Set<DownloadResponse> certificate;
	
}
