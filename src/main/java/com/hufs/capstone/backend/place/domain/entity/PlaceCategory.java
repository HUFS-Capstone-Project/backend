package com.hufs.capstone.backend.place.domain.entity;

import com.hufs.capstone.backend.global.common.entity.AuditableEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Entity
@Table(
		name = "place_category",
		uniqueConstraints = @UniqueConstraint(name = "uk_place_category_code", columnNames = "code")
)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class PlaceCategory extends AuditableEntity {

	@Column(nullable = false, length = 50)
	private String code;

	@Column(nullable = false, length = 50)
	private String name;

	@Column(nullable = false)
	private Integer sortOrder;

	@Column(nullable = false)
	private boolean isActive;

	private PlaceCategory(String code, String name, Integer sortOrder, boolean isActive) {
		this.code = code;
		this.name = name;
		this.sortOrder = sortOrder;
		this.isActive = isActive;
	}

	public static PlaceCategory create(String code, String name, Integer sortOrder, boolean isActive) {
		return new PlaceCategory(code, name, sortOrder, isActive);
	}

	public void updateMetadata(String name, Integer sortOrder, boolean isActive) {
		this.name = name;
		this.sortOrder = sortOrder;
		this.isActive = isActive;
	}
}
