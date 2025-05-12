package com.htc.licenseapproval.entity;

import java.time.LocalDateTime;
import java.util.Set;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.htc.licenseapproval.entity.idgenerator.HeaderId;
import com.htc.licenseapproval.enums.RequestType;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OneToOne;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Getter
@Setter
@NoArgsConstructor
public class RequestHeader extends BaseEntity{

	@Id
	@HeaderId
	private String requestHeaderId;

	@Column(nullable = false)
	private String requestorName;

	@Enumerated(EnumType.STRING)
	private RequestType requestType;

	@ManyToOne
	@JoinColumn(name = "bu_id")
	private BUdetails buDetails;

	@Column(name = "approved_manager")
	private String approvedBy;
	
	@Column(nullable = false)
	private String businessNeed;

	@OneToOne(cascade = CascadeType.ALL, fetch = FetchType.EAGER)
	private UploadedFile approvalMail;
	
	@OneToOne(cascade = CascadeType.ALL, fetch = FetchType.EAGER)
	private UploadedFile excelFile;
	
	@OneToMany(mappedBy = "requestHeader", fetch = FetchType.EAGER)
	private Set<RequestDetails> requestDetails;
	
	@JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
	private LocalDateTime licenseRequiredDate;
	

}
