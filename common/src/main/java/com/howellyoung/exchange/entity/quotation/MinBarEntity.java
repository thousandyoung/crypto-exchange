package com.howellyoung.exchange.entity.quotation;

import com.howellyoung.exchange.entity.base.BaseBarEntity;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;

/**
 * Store bars of minute.
 */
@Entity
@Table(name = "min_bars")
public class MinBarEntity extends BaseBarEntity {

}
