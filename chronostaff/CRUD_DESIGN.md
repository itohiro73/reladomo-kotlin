# ChronoStaff CRUD UI/UX Design

## Design Philosophy

**Core Principle**: Hide Reladomo's bitemporal complexity from end users.

Users should only think about **"When is this effective?"** (Business Date). The system automatically manages Processing Time (audit trail).

### What Users See
- **Single time concept**: "Effective Date" (実効日)
- Simple forms asking "When should this change take effect?"
- Intuitive workflows for employee management

### What System Handles Automatically
- Processing Time (PROCESSING_FROM/THRU)
- Bitemporal chaining when updating records
- Audit trail preservation
- Version management

## Three-Phase Implementation Plan

### Phase 1: MVP - Initial Setup and Basic Creation

**Goal**: Enable creating organization structure and employees from scratch.

#### 1.1 Initial Setup Wizard

**User Flow**:
```
Step 1: Company Information
  - Company Name
  - Timezone Settings (default: JST)

Step 2: Create Positions
  - Position Name (例: "社長", "部長", "マネージャー", "メンバー")
  - Level (1-10, higher = more senior)
  - Description

Step 3: Create Departments
  - Department Name (例: "経営企画部", "開発部", "営業部")
  - Description

Step 4: Review & Confirm
  - Show summary of positions and departments
  - "Start Using ChronoStaff" button
```

**Backend API**:
```kotlin
POST /api/setup
{
  "companyName": "株式会社サンプル",
  "positions": [
    {"name": "社長", "level": 10, "description": "最高経営責任者"},
    {"name": "部長", "level": 7, "description": "部門責任者"}
  ],
  "departments": [
    {"name": "経営企画部", "description": "経営戦略立案"}
  ]
}
```

**Implementation Notes**:
- Positions and Departments are non-temporal (no time dimensions)
- Simple INSERT operations
- Processing Time not relevant for these master data

#### 1.2 Employee Addition Form

**User Flow**:
```
Add New Employee Form:
  Basic Information:
    - Employee Number (自動生成または手動入力)
    - Name
    - Email
    - Hire Date (入社日)

  Initial Assignment:
    - Department (dropdown)
    - Position (dropdown)
    - Effective Date (実効日, defaults to hire date)

  Initial Salary:
    - Amount
    - Currency (default: JPY)
    - Effective Date (実効日, defaults to hire date)

  [Submit] button
```

**UI Design**:
```
┌─────────────────────────────────────────┐
│ 新規社員登録                              │
├─────────────────────────────────────────┤
│ 基本情報                                  │
│ ├─ 社員番号: [EMP-0001]                  │
│ ├─ 氏名: [         ]                     │
│ ├─ メール: [         ]                   │
│ └─ 入社日: [📅 2025-01-15]              │
│                                          │
│ 配属情報                                  │
│ ├─ 部署: [▼ 開発部]                     │
│ ├─ 役職: [▼ マネージャー]                │
│ └─ 実効日: [📅 2025-01-15]              │
│                                          │
│ 給与情報                                  │
│ ├─ 金額: [500000]                       │
│ ├─ 通貨: [JPY ▼]                        │
│ └─ 実効日: [📅 2025-01-15]              │
│                                          │
│          [キャンセル]  [登録する]         │
└─────────────────────────────────────────┘
```

**Backend API**:
```kotlin
POST /api/employees
{
  "employeeNumber": "EMP-0001",
  "name": "山田太郎",
  "email": "yamada@example.com",
  "hireDate": "2025-01-15",
  "assignment": {
    "departmentId": 1,
    "positionId": 3,
    "effectiveDate": "2025-01-15",  // Business Date
    "updatedBy": "admin@example.com"
  },
  "salary": {
    "amount": 500000,
    "currency": "JPY",
    "effectiveDate": "2025-01-15",  // Business Date
    "updatedBy": "admin@example.com"
  }
}
```

**Backend Implementation**:
```kotlin
@PostMapping("/employees")
fun createEmployee(@RequestBody dto: EmployeeCreateDto): EmployeeDto {
    return MithraManagerProvider.getMithraManager().executeTransactionalCommand { _ ->
        // 1. Create Employee (non-temporal)
        val employee = Employee()
        employee.employeeNumber = dto.employeeNumber
        employee.name = dto.name
        employee.email = dto.email
        employee.hireDate = Timestamp.from(Instant.parse("${dto.hireDate}T00:00:00Z"))
        employee.insert()

        // 2. Create Assignment (bitemporal)
        val assignment = EmployeeAssignment()
        assignment.employeeId = employee.id
        assignment.departmentId = dto.assignment.departmentId
        assignment.positionId = dto.assignment.positionId
        assignment.updatedBy = dto.assignment.updatedBy

        // User-specified Business Date (converted from JST to UTC)
        val effectiveDate = LocalDate.parse(dto.assignment.effectiveDate)
        val businessFrom = effectiveDate.atStartOfDay().toInstant(ZoneOffset.ofHours(9))
        assignment.businessFromAttribute = Timestamp.from(businessFrom)
        assignment.businessThruAttribute = Timestamp.from(Instant.parse("9999-12-01T23:59:00Z"))

        // Reladomo automatically sets:
        // - PROCESSING_FROM = current server time (UTC)
        // - PROCESSING_THRU = infinity
        assignment.insert()

        // 3. Create Salary (bitemporal) - same pattern
        val salary = Salary()
        salary.employeeId = employee.id
        salary.amount = dto.salary.amount.toBigDecimal()
        salary.currency = dto.salary.currency
        salary.updatedBy = dto.salary.updatedBy
        // ... set business dates ...
        salary.insert()

        toDto(employee)
    }
}
```

**Key Implementation Detail**:
- User provides "Effective Date" (実効日) in their timezone (JST)
- Backend converts to UTC for BUSINESS_FROM
- BUSINESS_THRU set to infinity (9999-12-01 23:59:00)
- PROCESSING_FROM/THRU **automatically managed by Reladomo**

---

### Phase 2: Core Value - Transfers and Changes

**Goal**: Enable changing assignments and salaries with future-dating support.

#### 2.1 Transfer/Assignment Change Form

**User Flow**:
```
From Organization Chart or Employee Detail:
  [異動・配置転換] button

Transfer Form:
  Current Assignment (read-only):
    - Department: 開発部
    - Position: マネージャー
    - Since: 2025-01-15

  New Assignment:
    - Department: [▼ 営業部]
    - Position: [▼ 部長]
    - Effective Date: [📅 2025-04-01]
    - Reason: [昇進に伴う営業部への異動]

  [Preview] [Submit]
```

**UI Design**:
```
┌─────────────────────────────────────────┐
│ 山田太郎さんの異動・配置転換              │
├─────────────────────────────────────────┤
│ 現在の配属 📍                            │
│ ├─ 部署: 開発部                         │
│ ├─ 役職: マネージャー                    │
│ └─ 配属日: 2025-01-15                   │
│                                          │
│ 新しい配属 ✨                            │
│ ├─ 部署: [▼ 営業部]                    │
│ ├─ 役職: [▼ 部長]                      │
│ ├─ 実効日: [📅 2025-04-01]             │
│ │   └─ ℹ️ この日付から新しい配属が有効   │
│ └─ 理由: [                    ]         │
│                                          │
│ プレビュー                               │
│ ┌─────────────────────────────────────┐ │
│ │ 2025-01-15 ━━━━━━━ 2025-03-31       │ │
│ │ 開発部 / マネージャー                 │ │
│ │                                      │ │
│ │ 2025-04-01 ━━━━━━━━━━━━━━━→       │ │
│ │ 営業部 / 部長                        │ │
│ └─────────────────────────────────────┘ │
│                                          │
│          [キャンセル]  [異動を登録]      │
└─────────────────────────────────────────┘
```

**Backend API**:
```kotlin
POST /api/employees/{id}/transfer
{
  "newDepartmentId": 2,
  "newPositionId": 4,
  "effectiveDate": "2025-04-01",  // Business Date
  "reason": "昇進に伴う営業部への異動",
  "updatedBy": "hr@example.com"
}
```

**Backend Implementation**:
```kotlin
@PostMapping("/employees/{id}/transfer")
fun transferEmployee(
    @PathVariable id: Long,
    @RequestBody dto: TransferDto
): EmployeeAssignmentDto {
    return MithraManagerProvider.getMithraManager().executeTransactionalCommand { _ ->
        val effectiveDate = LocalDate.parse(dto.effectiveDate)
        val businessFrom = effectiveDate.atStartOfDay().toInstant(ZoneOffset.ofHours(9))
        val businessTimestamp = Timestamp.from(businessFrom)

        // CRITICAL: Find current assignment using AsOf query
        // This finds the record that will be valid at effectiveDate
        val operation = EmployeeAssignmentFinder.employeeId().eq(id)
            .and(EmployeeAssignmentFinder.businessDate().eq(businessTimestamp))
            .and(EmployeeAssignmentFinder.processingDate().equalsInfinity())

        val currentAssignment = EmployeeAssignmentFinder.findOne(operation)
            ?: throw ResponseStatusException(
                HttpStatus.NOT_FOUND,
                "No assignment found for effective date ${dto.effectiveDate}"
            )

        // Terminate current assignment at effectiveDate
        // Reladomo handles this through chaining:
        // 1. Old version: BUSINESS_THRU set to businessFrom, PROCESSING_THRU set to now
        // 2. Creates new version with BUSINESS_THRU = effectiveDate, PROCESSING_FROM = now
        currentAssignment.businessThruAttribute = businessTimestamp

        // Create new assignment starting from effectiveDate
        val newAssignment = EmployeeAssignment()
        newAssignment.employeeId = id
        newAssignment.departmentId = dto.newDepartmentId
        newAssignment.positionId = dto.newPositionId
        newAssignment.updatedBy = dto.updatedBy
        newAssignment.businessFromAttribute = businessTimestamp
        newAssignment.businessThruAttribute = Timestamp.from(Instant.parse("9999-12-01T23:59:00Z"))
        // PROCESSING_FROM/THRU automatically set by Reladomo
        newAssignment.insert()

        toDto(newAssignment)
    }
}
```

**Key Implementation Details**:
- Use AsOf query to find assignment valid at effective date
- Setting `businessThruAttribute` triggers Reladomo's chaining
- New record starts exactly where old one ends (no gaps)
- PROCESSING_FROM/THRU automatically managed

#### 2.2 Salary Adjustment Form

**Same pattern as Transfer**:
```
Salary Adjustment Form:
  Current Salary (read-only):
    - Amount: ¥500,000
    - Since: 2025-01-15

  New Salary:
    - Amount: [600000]
    - Effective Date: [📅 2025-04-01]
    - Reason: [昇進に伴う昇給]

  [Preview] [Submit]
```

**Backend Implementation**: Same pattern as transfer, using `Salary` entity.

#### 2.3 Scheduled Changes View

**Purpose**: Show future-dated changes that haven't taken effect yet.

**UI Design**:
```
┌─────────────────────────────────────────┐
│ 予定されている変更 🔮                    │
├─────────────────────────────────────────┤
│ 2025-04-01 予定                          │
│ ├─ 山田太郎: 開発部 → 営業部              │
│ │            マネージャー → 部長          │
│ ├─ 佐藤花子: ¥450,000 → ¥500,000        │
│ └─ [詳細] [編集] [キャンセル]            │
│                                          │
│ 2025-07-01 予定                          │
│ ├─ 鈴木一郎: 営業部 → 開発部              │
│ └─ [詳細] [編集] [キャンセル]            │
└─────────────────────────────────────────┘
```

**Backend API**:
```kotlin
GET /api/changes/scheduled?asOfDate=2025-10-01
// Returns all records where BUSINESS_FROM > today
// Shows what changes are planned for the future
```

---

### Phase 3: Advanced - Historical Corrections

**Goal**: Fix past data mistakes while preserving audit trail.

#### 3.1 Historical Data Correction Form

**User Flow**:
```
From Employee History Timeline:
  Click on any past record
  [このデータを修正] button

Correction Form:
  Recorded Data (what we thought):
    - Assignment on 2025-01-15: 開発部 / マネージャー
    - Recorded by: admin@example.com on 2025-01-10

  Corrected Data (what actually happened):
    - Assignment: [▼ 営業部] / [▼ マネージャー]
    - Correction Reason: [配属部署の記録ミス]

  [Preview Audit Trail] [Submit Correction]
```

**UI Design**:
```
┌─────────────────────────────────────────┐
│ 過去データの修正                          │
├─────────────────────────────────────────┤
│ 修正対象データ ⚠️                        │
│ ├─ 日付: 2025-01-15                     │
│ ├─ 記録内容: 開発部 / マネージャー        │
│ ├─ 記録者: admin@example.com            │
│ └─ 記録日時: 2025-01-10 10:30           │
│                                          │
│ 正しいデータ ✅                           │
│ ├─ 部署: [▼ 営業部]                    │
│ ├─ 役職: [▼ マネージャー]               │
│ ├─ 修正理由: [              ]           │
│ └─ 修正者: hr@example.com               │
│                                          │
│ 監査証跡プレビュー                        │
│ ┌─────────────────────────────────────┐ │
│ │ 2025-01-10 に記録された情報:          │ │
│ │   開発部 / マネージャー               │ │
│ │   ↓ 誤りと判明                       │ │
│ │                                      │ │
│ │ 2025-10-26 に修正:                   │ │
│ │   営業部 / マネージャー               │ │
│ │   (正しい配属を記録)                  │ │
│ └─────────────────────────────────────┘ │
│                                          │
│   ⚠️ 注意: 過去データの修正は監査証跡に    │
│   残ります。元の記録は削除されません。     │
│                                          │
│          [キャンセル]  [修正を登録]      │
└─────────────────────────────────────────┘
```

**Backend API**:
```kotlin
POST /api/employees/{id}/correct-assignment
{
  "targetBusinessDate": "2025-01-15",  // Which date to correct
  "correctedDepartmentId": 2,          //営業部
  "correctedPositionId": 3,            // マネージャー (same)
  "correctionReason": "配属部署の記録ミス",
  "updatedBy": "hr@example.com"
}
```

**Backend Implementation**:
```kotlin
@PostMapping("/employees/{id}/correct-assignment")
fun correctAssignment(
    @PathVariable id: Long,
    @RequestBody dto: CorrectionDto
): EmployeeAssignmentDto {
    return MithraManagerProvider.getMithraManager().executeTransactionalCommand { _ ->
        val targetDate = LocalDate.parse(dto.targetBusinessDate)
        val businessTimestamp = Timestamp.from(
            targetDate.atStartOfDay().toInstant(ZoneOffset.ofHours(9))
        )

        // Find the record we want to correct
        val operation = EmployeeAssignmentFinder.employeeId().eq(id)
            .and(EmployeeAssignmentFinder.businessDate().eq(businessTimestamp))
            .and(EmployeeAssignmentFinder.processingDate().equalsInfinity())

        val existing = EmployeeAssignmentFinder.findOne(operation)
            ?: throw ResponseStatusException(
                HttpStatus.NOT_FOUND,
                "No assignment found at ${dto.targetBusinessDate}"
            )

        // CRITICAL: Update the business data
        // Reladomo will:
        // 1. Terminate old version (PROCESSING_THRU = now)
        // 2. Create new version (PROCESSING_FROM = now, PROCESSING_THRU = infinity)
        // 3. BUSINESS_FROM/THRU stay the same! (same business validity period)
        existing.departmentId = dto.correctedDepartmentId
        existing.positionId = dto.correctedPositionId
        existing.updatedBy = dto.updatedBy

        // The old version remains in database with PROCESSING_THRU set
        // This preserves audit trail: "We thought X from time T1 to T2"

        toDto(existing)
    }
}
```

**Critical Distinction**:
- **Transfer** (new business event): BUSINESS_FROM/THRU changes, split into two records
- **Correction** (fixing mistake): BUSINESS_FROM/THRU preserved, PROCESSING_FROM/THRU changes

#### 3.2 Audit Trail Viewer

**Purpose**: Show complete history of what we knew when.

**UI Design**:
```
┌─────────────────────────────────────────┐
│ 山田太郎さんの配属履歴（完全版）           │
├─────────────────────────────────────────┤
│ 2025-01-15 〜 2025-03-31                │
│ ┌─────────────────────────────────────┐ │
│ │ 📍 開発部 / マネージャー              │ │
│ │                                      │ │
│ │ 記録履歴:                            │ │
│ │ ・2025-01-10 admin@example.com      │ │
│ │   → "開発部" と記録                  │ │
│ │ ・2025-10-26 hr@example.com         │ │
│ │   → "営業部" に修正                  │ │
│ │   理由: 配属部署の記録ミス            │ │
│ └─────────────────────────────────────┘ │
│                                          │
│ 2025-04-01 〜 現在                       │
│ ┌─────────────────────────────────────┐ │
│ │ 📍 営業部 / 部長                     │ │
│ │                                      │ │
│ │ 記録履歴:                            │ │
│ │ ・2025-03-15 hr@example.com         │ │
│ │   → 昇進に伴う異動として登録          │ │
│ └─────────────────────────────────────┘ │
└─────────────────────────────────────────┘
```

**Backend API**:
```kotlin
GET /api/employees/{id}/assignment-history/full
// Returns ALL versions (not just current processing time)
// Shows complete audit trail of corrections
```

---

## Implementation Priorities

### Must Have (MVP)
1. ✅ Initial setup wizard
2. ✅ Employee addition form
3. ✅ Basic AsOf queries (already implemented)

### Should Have (Core Value)
4. Transfer/assignment change form
5. Salary adjustment form
6. Scheduled changes view

### Nice to Have (Advanced)
7. Historical data correction
8. Full audit trail viewer

## Technical Notes

### Timezone Conversion Pattern

**Frontend** (JST user input → UTC API):
```typescript
const effectiveDate = "2025-04-01";  // User input in JST
const utcTimestamp = new Date(effectiveDate + "T00:00:00+09:00").toISOString();
// Result: "2025-03-31T15:00:00Z"

const request = {
  effectiveDate: utcTimestamp,
  ...
};
```

**Backend** (UTC API → BUSINESS_FROM):
```kotlin
val effectiveDate = LocalDate.parse(dto.effectiveDate)  // Parse date part
val businessFrom = effectiveDate.atStartOfDay().toInstant(ZoneOffset.ofHours(9))
assignment.businessFromAttribute = Timestamp.from(businessFrom)
```

### Reladomo Chaining Triggers

**What triggers automatic chaining**:
1. Updating business temporal attributes (`businessFromAttribute`, `businessThruAttribute`)
2. Updating any business attribute on a bitemporal object

**What happens**:
1. Old version: PROCESSING_THRU = current server time
2. New version: PROCESSING_FROM = current server time, PROCESSING_THRU = infinity
3. Both versions kept in database (complete audit trail)

### AsOf Query Best Practices

**For finding current state**:
```kotlin
val operation = EntityFinder.id().eq(id)
    .and(EntityFinder.processingDate().equalsInfinity())
```

**For finding state at business date**:
```kotlin
val operation = EntityFinder.id().eq(id)
    .and(EntityFinder.businessDate().eq(targetDate))
    .and(EntityFinder.processingDate().equalsInfinity())
```

**For corrections (finding any version)**:
```kotlin
val operation = EntityFinder.id().eq(id)
    .and(EntityFinder.businessDate().eq(targetDate))
    .and(EntityFinder.processingDate().equalsInfinity())
// Then update properties - creates new processing version
```

## UI/UX Principles

### 1. Progressive Disclosure
- Show simple forms first
- Advanced features (corrections) behind extra clicks
- Preview before commit

### 2. Clear Mental Model
- One concept: "When is this effective?"
- Visual timeline representations
- Explicit "Effective Date" labels

### 3. Safety Rails
- Confirmation dialogs for corrections
- Preview of audit trail impact
- Clear warnings about historical changes

### 4. Helpful Defaults
- Effective Date defaults to "today" or "hire date"
- Pre-fill current values when changing
- Smart suggestions (same department, higher position = promotion)

### 5. Visual Feedback
- Timeline visualizations
- Color coding (past/current/future)
- Icons for different operation types (📍 transfer, 💰 salary, ✏️ correction)

---

## Success Metrics

### User Understanding
- Users should NOT need to know about "Processing Time"
- Users should understand "Effective Date" intuitively
- Users should feel confident making changes

### System Correctness
- No gaps in temporal data (every point in time covered)
- Complete audit trail (all changes preserved)
- Correct bitemporal chaining on every operation

### Demo Effectiveness
- Viewers understand how org chart is built
- Clear demonstration of temporal capabilities
- Complexity hidden but power demonstrated
