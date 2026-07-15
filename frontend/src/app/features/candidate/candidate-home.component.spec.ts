import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { CandidateHomeComponent } from './candidate-home.component';
import { CandidateProfile } from './candidate-profile.model';

describe('CandidateHomeComponent', () => {
  let fixture: ComponentFixture<CandidateHomeComponent>;
  let component: CandidateHomeComponent;
  let httpMock: HttpTestingController;

  const incompleteProfile: CandidateProfile = {
    email: 'candidate@test.ma',
    role: 'CANDIDATE',
    fullName: null,
    phone: null,
    sector: null,
    city: null,
    hasCv: false,
  };

  const completeProfile: CandidateProfile = {
    email: 'candidate@test.ma',
    role: 'CANDIDATE',
    fullName: 'Yasmine Alaoui',
    phone: '0612345678',
    sector: 'IT',
    city: 'Casablanca',
    hasCv: true,
  };

  beforeEach(() => {
    TestBed.configureTestingModule({
      imports: [CandidateHomeComponent],
      providers: [provideHttpClient(), provideHttpClientTesting()],
    });
    fixture = TestBed.createComponent(CandidateHomeComponent);
    component = fixture.componentInstance;
    httpMock = TestBed.inject(HttpTestingController);
  });

  afterEach(() => {
    httpMock.verify();
  });

  function loadWith(profile: CandidateProfile) {
    fixture.detectChanges();
    httpMock.expectOne('/api/v1/candidates/me').flush(profile);
  }

  it('loads the profile and populates the form', () => {
    loadWith(completeProfile);

    expect(component.form.value.fullName).toBe('Yasmine Alaoui');
    expect(component.form.value.city).toBe('Casablanca');
    expect(component.loading()).toBe(false);
  });

  it('flags an incomplete profile when fields or CV are missing', () => {
    loadWith(incompleteProfile);

    expect(component.incomplete()).toBe(true);
  });

  it('does not flag a fully completed profile', () => {
    loadWith(completeProfile);

    expect(component.incomplete()).toBe(false);
  });

  it('saves the form via PUT and updates the profile', () => {
    loadWith(incompleteProfile);

    component.form.setValue({ fullName: 'New Name', phone: '0600000000', sector: 'IT', city: 'Rabat' });
    component.save();

    const req = httpMock.expectOne('/api/v1/candidates/me');
    expect(req.request.method).toBe('PUT');
    expect(req.request.body).toEqual({ fullName: 'New Name', phone: '0600000000', sector: 'IT', city: 'Rabat' });
    req.flush({ ...incompleteProfile, fullName: 'New Name', phone: '0600000000', sector: 'IT', city: 'Rabat' });

    expect(component.saved()).toBe(true);
    expect(component.incomplete()).toBe(true); // still missing CV
  });

  it('uploads a CV and clears the incomplete flag once all fields are set', () => {
    loadWith(completeProfile);
    expect(component.incomplete()).toBe(false);

    const file = new File(['%PDF-1.4'], 'cv.pdf', { type: 'application/pdf' });
    component.onCvSelected({ target: { files: [file], value: '' } } as unknown as Event);

    const req = httpMock.expectOne('/api/v1/candidates/me/cv');
    expect(req.request.method).toBe('POST');
    req.flush(completeProfile);

    expect(component.profile()?.hasCv).toBe(true);
    expect(component.cvUploading()).toBe(false);
  });

  it('surfaces an error when CV upload is rejected', () => {
    loadWith(incompleteProfile);

    const file = new File(['not a pdf'], 'cv.pdf', { type: 'application/pdf' });
    component.onCvSelected({ target: { files: [file], value: '' } } as unknown as Event);

    const req = httpMock.expectOne('/api/v1/candidates/me/cv');
    req.flush('File is not a valid PDF', { status: 400, statusText: 'Bad Request' });

    expect(component.cvError()).toBe('PDF only, max 5MB');
    expect(component.cvUploading()).toBe(false);
  });
});
