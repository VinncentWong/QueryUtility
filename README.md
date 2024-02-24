# Query Utility
Query Utility is a library that I created with the aim of simplifying and shortening several processes in calling the repository. Instead of creating many methods, with this library, we can create several fixed methods that will be called from other layers but can still adjust to the needs of the client. 

# Requirement
- Java 1.8 or higher

# How to use it?
Let's say we have a class called User
```Java
@Entity
@NoArgsConstructor
@AllArgsConstructor
@DynamicInsert
@Builder
@ToString
@Setter
@Getter
@Table(name = "user")
public class User {

    @Id
    @Column(name = "account_number")
    private String accountNumber;

    @Column(name = "balance")
    private BigInteger balance;

    @Column(name = "name")
    private String name;

    @Column(name = "pin")
    private String pin;

    @Column(name = "created_at")
    @CreationTimestamp
    @JsonFormat(shape = JsonFormat.Shape.STRING)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    @UpdateTimestamp
    @JsonFormat(shape = JsonFormat.Shape.STRING)
    private LocalDateTime updatedAt;

    @Column(name = "deleted_at")
    @JsonFormat(shape = JsonFormat.Shape.STRING)
    private LocalDateTime deletedAt;

    @Column(name = "is_active")
    private Boolean isActive;
}

```
Now, let's say we can do a fetch of User class based on account number field.
> You can make a new class called UserParam
```java
@Builder
@Setter
@Getter
@ToString
public class UserParam {

    @ParamColumn(name = "account_number")
    private String accountNumber;

    @ParamColumn(name = "account_number")
    private List<String> accountNumbers;

    @ParamColumn(name = "is_active")
    private Boolean isActive;
  
    private HttpResponse.PaginationParam pgParam;
}
```

Now, what you have to do just
```java
public RepositoryData<User> get(UserParam param) {
        var query = QueryUtil
                .generateQuery(em, User.class, param);

        var res = query.getSingleResult();

        log.info("catch get result: {}", res);

        return RepositoryData
                .<User>builder()
                .data(res)
                .build();
    }
```

Now, when your UserParam class has accountNumber with value "abcde" then it will make a query
```
SELECT ... FROM `user` WHERE user.account_number=? with ? is equal to 'abcde'
```

What if the we have accountNumbers instead of accountNumber? Then, it will make a query
```
SELECT ... FROM `user` WHERE user.account_number IN ? with ? is equal to 'abcde'
```

# Notes
On every Param class that you make, don't forget to use `@ParamColumn` to name the **actual** column name on your database