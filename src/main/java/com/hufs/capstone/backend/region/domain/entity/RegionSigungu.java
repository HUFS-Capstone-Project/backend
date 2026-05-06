package com.hufs.capstone.backend.region.domain.entity;

import com.hufs.capstone.backend.global.common.entity.AuditableEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Entity
@Table(
		name = "region_sigungu",
		indexes = {
			@Index(name = "idx_region_sigungu_sido_code", columnList = "sido_code"),
			@Index(name = "idx_region_sigungu_sido_active_display_order", columnList = "sido_code, active, display_order")
		},
		uniqueConstraints = {
			@UniqueConstraint(name = "uk_region_sigungu_code", columnNames = "code")
		}
)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class RegionSigungu extends AuditableEntity {

	@Column(nullable = false, length = 5)
	private String code;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "sido_code", referencedColumnName = "code", nullable = false)
	private RegionSido sido;

	@Column(nullable = false, length = 50)
	private String name;

	@Column(name = "display_order", nullable = false)
	private Integer displayOrder;

	@Column(nullable = false)
	private boolean active;

	private RegionSigungu(RegionSido sido, String code, String name, Integer displayOrder, boolean active) {
		this.sido = sido;
		this.code = code;
		this.name = name;
		this.displayOrder = displayOrder;
		this.active = active;
	}

	public static RegionSigungu create(RegionSido sido, String code, String name, Integer displayOrder, boolean active) {
		return new RegionSigungu(sido, code, name, displayOrder, active);
	}

	public void updateMetadata(RegionSido sido, String name, Integer displayOrder, boolean active) {
		this.sido = sido;
		this.name = name;
		this.displayOrder = displayOrder;
		this.active = active;
	}
}
