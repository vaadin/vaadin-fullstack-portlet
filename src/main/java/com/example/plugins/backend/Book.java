package com.example.plugins.backend;

import java.util.Date;

import javax.persistence.Entity;
import javax.persistence.Temporal;
import javax.persistence.TemporalType;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;

@Entity
public class Book extends AbstractEntity {

    @Temporal(TemporalType.TIMESTAMP)
    private Date publishDate;

    @NotNull(message = "Name is required")
    @Size(min = 3, max = 50, message = "name must be longer than 3 and less than 40 characters")
    private String name;
    
    private String description;
    
    private String loanedBy;

    public String getLoanedBy() {
        return loanedBy;
    }

    public void setLoanedBy(String loanedBy) {
        this.loanedBy = loanedBy;
    }
    
    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public Date getPublishDate() {
        return publishDate;
    }

    public void setPublishDate(Date publishDate) {
        this.publishDate = publishDate;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

}
