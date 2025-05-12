package com.htc.licenseapproval.dto;

import java.util.Set;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.htc.licenseapproval.entity.UploadedFile;
import com.htc.licenseapproval.enums.RequestType;

import jakarta.validation.constraints.NotNull;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class NewRequestListDTO {

	private String requestorName;
	private RequestType requestType;
	private BUdetailsDTO buDetails;
	private String approvedBy;
	private Set<RequestDetailsDTO> requestDetails;
	@JsonIgnore
	private UploadedFile excelFile;
	@JsonIgnore
	private UploadedFile approvalMail;
	private String businessNeed;
	private String licenseRequiredDate;

 }
