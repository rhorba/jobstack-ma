import { Component, inject, signal } from '@angular/core';
import { ReactiveFormsModule, FormBuilder, Validators } from '@angular/forms';
import { Router, RouterLink } from '@angular/router';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatButtonModule } from '@angular/material/button';
import { MatSelectModule } from '@angular/material/select';
import { AuthService } from '../../core/auth/auth.service';

@Component({
  selector: 'app-register',
  standalone: true,
  imports: [ReactiveFormsModule, RouterLink, MatFormFieldModule, MatInputModule, MatButtonModule, MatSelectModule],
  templateUrl: './register.component.html',
})
export class RegisterComponent {
  private fb = inject(FormBuilder);
  private auth = inject(AuthService);
  private router = inject(Router);

  readonly error = signal<string | null>(null);
  readonly success = signal(false);

  form = this.fb.group({
    email: ['', [Validators.required, Validators.email]],
    password: ['', [Validators.required, Validators.minLength(10)]],
    role: ['CANDIDATE' as 'CANDIDATE' | 'EMPLOYER', [Validators.required]],
  });

  submit(): void {
    if (this.form.invalid) return;
    this.error.set(null);
    const { email, password, role } = this.form.getRawValue();

    this.auth.register(email!, password!, role!).subscribe({
      next: () => {
        this.success.set(true);
        this.router.navigateByUrl('/login');
      },
      error: (err) => {
        this.error.set(err.status === 409 ? 'That email is already registered' : 'Registration failed');
      },
    });
  }
}
