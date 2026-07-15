import { HttpClient } from '@angular/common/http';
import { Component, OnInit, computed, inject, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { MatButtonModule } from '@angular/material/button';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { CandidateProfile } from './candidate-profile.model';

@Component({
  selector: 'app-candidate-home',
  standalone: true,
  imports: [ReactiveFormsModule, MatFormFieldModule, MatInputModule, MatButtonModule],
  templateUrl: './candidate-home.component.html',
})
export class CandidateHomeComponent implements OnInit {
  private http = inject(HttpClient);
  private fb = inject(FormBuilder);

  readonly profile = signal<CandidateProfile | null>(null);
  readonly loading = signal(true);
  readonly saving = signal(false);
  readonly saved = signal(false);
  readonly saveError = signal<string | null>(null);
  readonly cvUploading = signal(false);
  readonly cvError = signal<string | null>(null);

  readonly incomplete = computed(() => {
    const p = this.profile();
    if (!p) return false;
    return !p.fullName || !p.phone || !p.sector || !p.city || !p.hasCv;
  });

  form = this.fb.group({
    fullName: ['', [Validators.required, Validators.maxLength(200)]],
    phone: ['', [Validators.required, Validators.maxLength(30)]],
    sector: ['', [Validators.required, Validators.maxLength(50)]],
    city: ['', [Validators.required, Validators.maxLength(100)]],
  });

  ngOnInit(): void {
    this.http.get<CandidateProfile>('/api/v1/candidates/me').subscribe((profile) => {
      this.applyProfile(profile);
      this.loading.set(false);
    });
  }

  save(): void {
    if (this.form.invalid) return;
    this.saving.set(true);
    this.saveError.set(null);
    this.saved.set(false);

    this.http.put<CandidateProfile>('/api/v1/candidates/me', this.form.getRawValue()).subscribe({
      next: (profile) => {
        this.applyProfile(profile);
        this.saving.set(false);
        this.saved.set(true);
      },
      error: () => {
        this.saving.set(false);
        this.saveError.set('Could not save your profile. Please check the fields and try again.');
      },
    });
  }

  onCvSelected(event: Event): void {
    const input = event.target as HTMLInputElement;
    const file = input.files?.[0];
    if (!file) return;

    this.cvUploading.set(true);
    this.cvError.set(null);
    const formData = new FormData();
    formData.append('file', file);

    this.http.post<CandidateProfile>('/api/v1/candidates/me/cv', formData).subscribe({
      next: (profile) => {
        this.applyProfile(profile);
        this.cvUploading.set(false);
      },
      error: () => {
        this.cvUploading.set(false);
        this.cvError.set('PDF only, max 5MB');
      },
    });
    input.value = '';
  }

  private applyProfile(profile: CandidateProfile): void {
    this.profile.set(profile);
    this.form.patchValue({
      fullName: profile.fullName ?? '',
      phone: profile.phone ?? '',
      sector: profile.sector ?? '',
      city: profile.city ?? '',
    });
  }
}
