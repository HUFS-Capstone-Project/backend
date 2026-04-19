package com.hufs.capstone.backend.place.domain.entity;

import com.hufs.capstone.backend.global.common.entity.AuditableEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.util.Objects;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Entity
@Table(
		name = "place_tag",
		uniqueConstraints = {
				@UniqueConstraint(name = "uk_place_tag_category_code", columnNames = {"category_id", "code"})
		}
)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class PlaceTag extends AuditableEntity {

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "category_id", nullable = false)
	private PlaceCategory category;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "tag_group_id")
	private PlaceTagGroup tagGroup;

	@Column(nullable = false, length = 50)
	private String code;

	@Column(nullable = false, length = 50)
	private String name;

	@Column(nullable = false)
	private Integer sortOrder;

	@Column(nullable = false)
	private boolean isActive;

	private PlaceTag(
			PlaceCategory category,
			PlaceTagGroup tagGroup,
			String code,
			String name,
			Integer sortOrder,
			boolean isActive
	) {
		validateTagGroup(category, tagGroup);
		this.category = category;
		this.tagGroup = tagGroup;
		this.code = code;
		this.name = name;
		this.sortOrder = sortOrder;
		this.isActive = isActive;
	}

	public static PlaceTag create(
			PlaceCategory category,
			PlaceTagGroup tagGroup,
			String code,
			String name,
			Integer sortOrder,
			boolean isActive
	) {
		return new PlaceTag(category, tagGroup, code, name, sortOrder, isActive);
	}

	public void updateMetadata(PlaceTagGroup tagGroup, String name, Integer sortOrder, boolean isActive) {
		validateTagGroup(this.category, tagGroup);
		this.tagGroup = tagGroup;
		this.name = name;
		this.sortOrder = sortOrder;
		this.isActive = isActive;
	}

	private static void validateTagGroup(PlaceCategory category, PlaceTagGroup tagGroup) {
		if (tagGroup == null) {
			return;
		}
		PlaceCategory groupCategory = tagGroup.getCategory();
		Long categoryId = category.getId();
		Long groupCategoryId = groupCategory.getId();
		if (categoryId != null && groupCategoryId != null) {
			if (!Objects.equals(categoryId, groupCategoryId)) {
				throw new IllegalArgumentException("Tag group category mismatch.");
			}
			return;
		}
		if (groupCategory != category) {
			throw new IllegalArgumentException("Tag group category mismatch.");
		}
	}
}
