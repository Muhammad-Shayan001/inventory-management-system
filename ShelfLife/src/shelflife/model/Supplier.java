package shelflife.model;

import java.util.Objects;

/**
 * Supplier entity for managing vendor information and purchase / receiving orders.
 */
public class Supplier {
    private String supplierId;
    private String name;
    private String contactPerson;
    private String phone;
    private String email;
    private String address;
    private boolean active;

    public Supplier() {
        this.active = true;
    }

    public Supplier(String supplierId, String name, String contactPerson, String phone, String email, String address, boolean active) {
        setSupplierId(supplierId);
        setName(name);
        this.contactPerson = contactPerson != null ? contactPerson.trim() : "";
        this.phone = phone != null ? phone.trim() : "";
        this.email = email != null ? email.trim() : "";
        this.address = address != null ? address.trim() : "";
        this.active = active;
    }

    public String getSupplierId() {
        return supplierId;
    }

    public void setSupplierId(String supplierId) {
        if (supplierId == null || supplierId.trim().isEmpty()) {
            throw new IllegalArgumentException("Supplier ID cannot be null or empty.");
        }
        this.supplierId = supplierId.trim().toUpperCase();
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        if (name == null || name.trim().isEmpty()) {
            throw new IllegalArgumentException("Supplier name cannot be null or empty.");
        }
        this.name = name.trim();
    }

    public String getContactPerson() {
        return contactPerson;
    }

    public void setContactPerson(String contactPerson) {
        this.contactPerson = contactPerson != null ? contactPerson.trim() : "";
    }

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone != null ? phone.trim() : "";
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email != null ? email.trim() : "";
    }

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address != null ? address.trim() : "";
    }

    public boolean isActive() {
        return active;
    }

    public void setActive(boolean active) {
        this.active = active;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Supplier supplier = (Supplier) o;
        return Objects.equals(supplierId, supplier.supplierId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(supplierId);
    }

    @Override
    public String toString() {
        return name + " (" + supplierId + ")";
    }
}
