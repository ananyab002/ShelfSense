package com.shelf_sense_backend.repo;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.shelf_sense_backend.model.UserEntity;


@Repository
public interface UserRepository extends JpaRepository<UserEntity, Long> {

	UserEntity getByEmail(String email);
	Optional<UserEntity>  findByEmail(String email);
}
