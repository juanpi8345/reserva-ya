package com.reservaya.notification_service.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Getter @Setter
@NoArgsConstructor
@AllArgsConstructor
@Table(name="user_contacts")
public class UserContact {
	@Id
	@Column(name="user_id",nullable=false)
	private Long userId;
	@Column(nullable=false)
	private String phone;
	@Column(nullable=false)
	private String email;
}
