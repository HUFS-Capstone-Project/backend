package com.hufs.capstone.backend.region.domain.entity;

import com.hufs.capstone.backend.global.common.entity.AuditableEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Entity
@Table(
		name = "region_sido",
		indexes = {
			@Index(name = "idx_region_sido_active_display_order", columnList = "active, display_order")
		},
		uniqueConstraints = {
			@UniqueConstraint(name = "uk_region_sido_code", columnNames = "code")
		}
)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class RegionSido extends AuditableEntity {

	@Column(nullable = false, length = 2)
	private String code;

	@Column(nullable = false, length = 50)
	private String name;

	@Column(name = "display_order", nullable = false)
	private Integer displayOrder;

	@Column(nullable = false)
	private boolean active;

	private RegionSido(String code, String name, Integer displayOrder, boolean active) {
		this.code = code;
		this.name = name;
		this.displayOrder = displayOrder;
		this.active = active;
	}

	public static RegionSido create(String code, String name, Integer displayOrder, boolean active) {
		return new RegionSido(code, name, displayOrder, active);
	}

	public void updateMetadata(String name, Integer displayOrder, boolean active) {
		this.name = name;
		this.displayOrder = displayOrder;
		this.active = active;
	}
}
