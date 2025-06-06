package com.htc.licenseapproval.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.htc.licenseapproval.entity.RequestHeader;

@Repository
public interface RequestHeaderRepository extends JpaRepository<RequestHeader, String> {

}
