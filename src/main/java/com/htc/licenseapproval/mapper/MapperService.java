package com.htc.licenseapproval.mapper;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.htc.licenseapproval.dto.BUdetailsDTO;
import com.htc.licenseapproval.dto.CoursesDTO;
import com.htc.licenseapproval.dto.DownloadResponse;
import com.htc.licenseapproval.dto.LicenseDetailsDTO;
import com.htc.licenseapproval.dto.NewRequestListDTO;
import com.htc.licenseapproval.dto.RequestDetailsDTO;
import com.htc.licenseapproval.dto.RequestResponseDTO;
import com.htc.licenseapproval.entity.BUdetails;
import com.htc.licenseapproval.entity.Courses;
import com.htc.licenseapproval.entity.LicenseDetails;
import com.htc.licenseapproval.entity.RequestDetails;
import com.htc.licenseapproval.entity.RequestHeader;
import com.htc.licenseapproval.entity.UploadedFile;
import com.htc.licenseapproval.utils.DateFormatter;

@Service
public class MapperService {

	@Autowired
	private ModelMapper modelMapper;

	public RequestResponseDTO toResponseDTO(RequestHeader requestList) {

		RequestResponseDTO requestResponseDTO = modelMapper.map(requestList, RequestResponseDTO.class);

		if(requestList.getApprovalMail() !=null) {
		DownloadResponse downloadResponse = new DownloadResponse();
		downloadResponse.setFilename(requestList.getApprovalMail().getFileName());
		downloadResponse
				.setUrl("/licenseApproval/approvalRequest/download/approvalMail/" + requestList.getRequestHeaderId());
		requestResponseDTO.setApprovalMail(downloadResponse);
		}
		if (requestList.getExcelFile() != null) {
			DownloadResponse downloadResponse2 = new DownloadResponse();
			downloadResponse2.setFilename(requestList.getExcelFile().getFileName());
			downloadResponse2
					.setUrl("/licenseApproval/approvalRequest/download/requestlistexcel/" + requestList.getRequestHeaderId());
			requestResponseDTO.setExcelFile(downloadResponse2);
		}

		Set<RequestDetailsDTO> requestDetailsDTOs = requestList.getRequestDetails().stream()
				.map(this::toRequestDetailsDTO).collect(Collectors.toSet());
		
		requestResponseDTO.setRequestDetails(requestDetailsDTOs);

		return requestResponseDTO;

	}

	public RequestDetailsDTO toRequestDetailsDTO(RequestDetails requestList) {

		RequestDetailsDTO detailsDTO = modelMapper.map(requestList, RequestDetailsDTO.class);
		if (requestList.getCourses() != null) {
			Set<CoursesDTO> coursesDTOs = requestList.getCourses().stream().map(this::toCourseDTO)
					.collect(Collectors.toSet());
			detailsDTO.setCourses(coursesDTOs);
		}

		return detailsDTO;
	}

	public RequestResponseDTO toResponseDTO(RequestDetails requestList) {

		RequestResponseDTO requestResponseDTO = this.toResponseDTO(requestList.getRequestHeader());
		RequestDetailsDTO detailsDTO = this.toRequestDetailsDTO(requestList);
		Set<RequestDetailsDTO> dtos = new HashSet<>();
		dtos.add(detailsDTO);
		requestResponseDTO.setRequestDetails(dtos);
		return requestResponseDTO;

	}

	public RequestHeader toRequestList(RequestResponseDTO requestResponseDTO) {

		return modelMapper.map(requestResponseDTO, RequestHeader.class);

	}

	public CoursesDTO toCourseDTO(Courses courses) {

		CoursesDTO dto = modelMapper.map(courses, CoursesDTO.class);
		Set<DownloadResponse> certificate = new HashSet<>();
		if(courses.getCertificates()!=null && courses.isCertificateDone()) {
		for (UploadedFile file : courses.getCertificates()) {
			DownloadResponse downloadResponse = new DownloadResponse();
			downloadResponse.setFilename(file.getFileName());
			downloadResponse.setUrl("/licenseApproval/approvalRequest/download/certificates/" + file.getId());
			certificate.add(downloadResponse);
		}
		}
		dto.setCertificate(certificate);

		return dto;
	}

	public RequestHeader toRequestList(NewRequestListDTO requestList) {
		LocalDate lacalDate = LocalDate.parse(requestList.getLicenseRequiredDate());
		LocalDateTime date = DateFormatter.normaliseDate(lacalDate.atStartOfDay());
		RequestHeader header = modelMapper.map(requestList, RequestHeader.class);
		header.setLicenseRequiredDate(date);
		return header;
	}

	public RequestResponseDTO toResponse(NewRequestListDTO requestList) {
		return modelMapper.map(requestList, RequestResponseDTO.class);
	}

	public NewRequestListDTO toNewRequestList(RequestHeader requestList) {
		return modelMapper.map(requestList, NewRequestListDTO.class);
	}

	public RequestDetails toRequestDetails(RequestDetailsDTO requestList) {
		return modelMapper.map(requestList, RequestDetails.class);
	}

	public LicenseDetailsDTO toLicenseDTO(LicenseDetails detailsDTO) {
		return modelMapper.map(detailsDTO, LicenseDetailsDTO.class);
	}

	
	public BUdetailsDTO toBUdetailsDTO(BUdetails bUdetails) {
		return modelMapper.map(bUdetails, BUdetailsDTO.class);
	}
}
