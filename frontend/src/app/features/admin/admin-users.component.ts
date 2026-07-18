import { HttpClient } from '@angular/common/http';
import { Component, inject, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { RouterLink } from '@angular/router';
import { MatButtonModule } from '@angular/material/button';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatSelectModule } from '@angular/material/select';
import { UserStatusResult } from './admin-user-status.model';

@Component({
  selector: 'app-admin-users',
  standalone: true,
  imports: [ReactiveFormsModule, RouterLink, MatButtonModule, MatFormFieldModule, MatInputModule, MatSelectModule],
  templateUrl: './admin-users.component.html',
})
export class AdminUsersComponent {
  private http = inject(HttpClient);
  private fb = inject(FormBuilder);

  readonly result = signal<UserStatusResult | null>(null);
  readonly error = signal<string | null>(null);
  readonly submitting = signal(false);

  form = this.fb.group({
    userId: ['', [Validators.required]],
    status: ['SUSPENDED', [Validators.required]],
  });

  submit(): void {
    if (this.form.invalid) return;
    const { userId, status } = this.form.getRawValue();
    this.submitting.set(true);
    this.error.set(null);
    this.result.set(null);

    this.http.put<UserStatusResult>(`/api/v1/admin/users/${userId}/status`, { status }).subscribe({
      next: (res) => {
        this.result.set(res);
        this.submitting.set(false);
      },
      error: () => {
        this.submitting.set(false);
        this.error.set('Could not update that user. Check the user ID and try again.');
      },
    });
  }
}
