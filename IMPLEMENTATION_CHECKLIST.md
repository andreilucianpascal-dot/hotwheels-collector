# HotWheels Collectors — Implementation Checklist (Agreed Scope)

Date: 2026-02-25

This document captures the agreed work items and the execution order.
Flow constraint: **Do not add extra user steps to the add-car flow** (save happens immediately after photos).

## 1) Browse UI cleanup (simple, high impact)
- Remove **"Check price"** functionality from **all Browse screens**.
- Keep **"Check price"** only in **My Collection**.

## 2) Browse cards layout polish (simple UI refactor)
- In Browse lists/cards:
  - Place **"Add to my collection"** button **directly under the thumbnail**.
  - Keep the **right side** reserved for **details** (clean, consistent alignment).
- Browse details shown must respect category-specific rules (Mainline vs Premium/others).

## 3) My Collection: total count + money features (local-only)
- Add **Total cars** indicator/button in My Collection (top area).
- Per-car details (My Collection → Car Details):
  - Add editable field/button: **"Price you paid for this car"** (maps to `purchasePrice`).
- My Collection summary:
  - Add **Total price paid** (sum of all `purchasePrice` for the current user).

## 4) Settings UX improvements (simple)
- Extend the **year selection dropdown** to include years up to **2100**.
- Change **font selection** from a long list into a **dropdown**.
- Make Settings rows/buttons:
  - **Same height**
  - **Consistent padding**
  - **Clean symmetry and ordering**
  - Avoid inconsistent wording like scattered "Change" labels.

## 5) Global Browse dedupe + Pending → Published (complex, do after 1–4)
Goal: prevent duplicates in the global Browse catalog while still allowing variants.

- Publishing rule:
  - A newly added car is saved immediately to **My Collection**.
  - If required fields are not completed, the global entry remains **Pending**.
  - When a user completes required fields in details, the car is **published automatically** to Browse.

- Required fields for publish:
  - **Mainline**: `year + color + model`
  - **Premium/TH/STH/Others (rest)**: `brand + model + year + color`

- Pending visibility:
  - Prefer the option that **prevents duplicates** (Pending visible in a separate place/tab/filter rather than mixing with Published).

- Canonical global uniqueness:
  - Use a deterministic **`variantKey`** (normalized) so only one document exists per variant.
  - If a user tries to publish an existing variant → show **"Already exists"** (no duplicate doc).

- Permissions:
  - After Published, only **admin (developer)** can modify global Browse canonical details.

## 6) Others: per-user local folders (last)
Goal: make Others (My Collection) customizable per user without causing global Browse confusion.

- Users can create their own folders/sections **only locally** (included in **Google Drive backup/restore**).
- No folder concept in global Browse.
- "Add to my collection" from Browse for Others saves into **Unfiled/Default**; user can later move it to any local folder.

