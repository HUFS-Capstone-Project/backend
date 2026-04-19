package com.hufs.capstone.backend.place.domain.entity;

import com.hufs.capstone.backend.global.common.entity.AuditableEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
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
		name = "place_tag_group",
		uniqueConstraints = {
				@UniqueConstraint(name = "uk_place_tag_group_category_code", columnNames = {"category_id", "code"})
		}
)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class PlaceTagGroup extends AuditableEntity {

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "category_id", nullable = false)
	private PlaceCategory category;

	@Column(nullable = false, length = 50)
	private String code;

	@Column(nullable = false, length = 50)
	private String name;

	@Column(nullable = false)
	private Integer sortOrder;

	@Column(nullable = false)
	private boolean isActive;

	private PlaceTagGroup(PlaceCategory category, String code, String name, Integer sortOrder, boolean isActive) {
		this.category = category;
		this.code = code;
		this.name = name;
		this.sortOrder = sortOrder;
		this.isActive = isActive;
	}

	public static PlaceTagGroup create(
			PlaceCategory category,
			String code,
			String name,
			Integer sortOrder,
			boolean isActive
	) {
		return new PlaceTagGroup(category, code, name, sortOrder, isActive);
	}

	public void updateMetadata(String name, Integer sortOrder, boolean isActive) {
		this.name = name;
		this.sortOrder = sortOrder;
		this.isActive = isActive;
	}
}
